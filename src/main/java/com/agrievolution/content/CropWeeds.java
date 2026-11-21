package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = AgriEvolution.MOD_ID)
public final class CropWeeds {
    private static final TagKey<Block> HOSTS = tag("agri_evolution", "weed_hosts");
    private static final TagKey<Block> EXCLUDED = tag("agri_evolution", "weed_immune");
    private static final TagKey<Block> CROPS = tag("minecraft", "crops");
    private static final TagKey<Block> COMMON_CROPS = tag("c", "crops");
    private static Set<Block> seedBlocks;
    private static TagKey<Block> tag(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
    @SubscribeEvent public static void tagsUpdated(TagsUpdatedEvent event) { seedBlocks = null; }
    private static Set<Block> seedBlocks() {
        if (seedBlocks == null) {
            Set<Block> blocks = new HashSet<>();
            for (var item : BuiltInRegistries.ITEM) {
                if (item instanceof BlockItem blockItem && SeedBreedingParents.taggedSeed(item.getDefaultInstance())) blocks.add(blockItem.getBlock());
            }
            seedBlocks = Set.copyOf(blocks);
        }
        return seedBlocks;
    }
    public static IntegerProperty ageProperty(BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && Set.of("age", "growth", "stage").contains(property.getName())) return integer;
        }
        return null;
    }
    public static int age(BlockState state) {
        var property = ageProperty(state);
        return property == null ? -1 : state.getValue(property);
    }
    public static boolean isHost(BlockState state) {
        if (state.isAir() || state.is(EXCLUDED)) return false;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return false;
        Block block = state.getBlock();
        return block instanceof CropBlock || block instanceof StemBlock || block instanceof NetherWartBlock
                || block instanceof CocoaBlock || block instanceof SweetBerryBushBlock || block instanceof PitcherCropBlock
                || state.is(HOSTS) || state.is(CROPS) || state.is(COMMON_CROPS) || seedBlocks().contains(block);
    }
    public static boolean growing(Level level, BlockPos pos, BlockState state) {
        if (!isHost(state)) return false;
        if (state.getBlock() instanceof CropBlock crop) return !crop.isMaxAge(state);
        var age = ageProperty(state);
        if (age != null) return state.getValue(age) < age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (state.getBlock() instanceof BonemealableBlock plant) return plant.isValidBonemealTarget(level, pos, state);
        // Explicit compatibility tags can opt in crops whose growth is stored in a block entity.
        return state.is(HOSTS);
    }
    public static boolean hasWeeds(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return (state.hasProperty(AgriCropBlock.WEEDY) && state.getValue(AgriCropBlock.WEEDY))
                || !level.getEntitiesOfClass(CropWeedEntity.class, new AABB(pos), e -> !e.isRemoved() && e.blockPosition().equals(pos)).isEmpty();
    }
    public static void clear(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(AgriCropBlock.WEEDY) && state.getValue(AgriCropBlock.WEEDY)) level.setBlock(pos, state.setValue(AgriCropBlock.WEEDY, false), 3);
        for (var weed : level.getEntitiesOfClass(CropWeedEntity.class, new AABB(pos), e -> e.blockPosition().equals(pos))) weed.discard();
    }
    private static void spawn(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof AgriCropBlock || !growing(level, pos, state) || hasWeeds(level, pos)) return;
        if (SoilCareData.mulched(level,pos.below()) && level.random.nextBoolean()) return;
        var weed = AEEntities.CROP_WEED.get().create(level);
        if (weed != null) { weed.attach(pos, state); level.addFreshEntity(weed); }
    }
    @SubscribeEvent public static void growth(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getState().getBlock() instanceof AgriCropBlock
                || !growing(level, event.getPos(), event.getState()) || event.getResult() != CropGrowEvent.Pre.Result.DEFAULT) return;
        if (level.random.nextInt(40) == 0) spawn(level, event.getPos(), event.getState());
        if (hasWeeds(level, event.getPos()) && level.random.nextBoolean()) event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }
    /** Fallback for mods that don't fire crop events: at most one loaded section per second per level. */
    @SubscribeEvent public static void scan(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0 || level.players().isEmpty()) return;
        long step = level.getGameTime() / 20;
        var players = level.players();
        var player = players.get((int) (step % players.size()));
        int phase = (int) ((step / players.size()) % 27);
        BlockPos center = player.blockPosition();
        int cx = (center.getX() >> 4) + phase % 3 - 1;
        int cz = (center.getZ() >> 4) + (phase / 3) % 3 - 1;
        int sy = (center.getY() >> 4) + phase / 9 - 1;
        var chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null || level.isOutsideBuildHeight(sy << 4)) return;
        var section = chunk.getSection(chunk.getSectionIndex(sy << 4));
        if (section.hasOnlyAir() || !section.maybeHas(s -> !(s.getBlock() instanceof AgriCropBlock) && isHost(s))) return;
        int attempts = 0;
        int offset = level.random.nextInt(4096);
        for (int i = 0; i < 4096; i++) {
            int cell = (offset + i) & 4095;
            int x = cell & 15, z = (cell >> 4) & 15, y = cell >> 8;
            BlockState state = section.getBlockState(x, y, z);
            if (state.getBlock() instanceof AgriCropBlock || !isHost(state) || level.random.nextInt(8) != 0) continue;
            spawn(level, new BlockPos((cx << 4) + x, (sy << 4) + y, (cz << 4) + z), state);
            if (++attempts >= 8) return;
        }
    }
    private CropWeeds() {}
}
