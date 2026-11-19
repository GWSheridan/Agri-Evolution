package com.agrievolution.content;

import com.agrievolution.farming.SoilResearch;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.SeedVariety;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class FieldToolGameTests {
    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static UseOnContext context(Player player, BlockPos pos) {
        return new UseOnContext(player, InteractionHand.OFF_HAND, hit(pos));
    }

    private static AECropBlockEntity plant(GameTestHelper h, BlockPos pos, int age) {
        h.getLevel().setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        h.getLevel().setBlock(pos, AEBlocks.TOMATO_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, age), 2);
        return (AECropBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static void fillInventory(Player player) {
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
    }

    private static List<ItemStack> dropped(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream().map(ItemEntity::getItem).toList();
    }

    private static void cross(Player player, BlockPos first, BlockPos second) {
        var book = player.getOffhandItem();
        player.setShiftKeyDown(true);
        book.getItem().useOn(context(player, first));
        player.setShiftKeyDown(false);
        book.getItem().useOn(context(player, second));
    }

    @GameTest(template = "empty")
    public static void compostRespectsBuildPermissions(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var crop = plant(h, pos, 2);
        int nutrition = crop.soilNutrition();
        for (var mode : new GameType[]{GameType.ADVENTURE, GameType.SPECTATOR}) {
            var player = h.makeMockPlayer(mode);
            player.getAbilities().mayBuild = false;
            var compost = new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 2);
            player.setItemInHand(InteractionHand.OFF_HAND, compost);
            compost.getItem().useOn(context(player, pos));
            h.assertTrue(h.getLevel().getBlockState(pos).getValue(AgriCropBlock.AGE) == 2
                    && compost.getCount() == 2 && crop.soilNutrition() == nutrition, "Denied compost preserves the crop and item");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void playerlessCompostCannotChangeTheCrop(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var crop = plant(h, pos, 2);
        int nutrition = crop.soilNutrition();
        var compost = new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 2);
        var context = new UseOnContext(h.getLevel(), null, InteractionHand.OFF_HAND, compost, hit(pos)) {};
        compost.getItem().useOn(context);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(AgriCropBlock.AGE) == 2
                && compost.getCount() == 2 && crop.soilNutrition() == nutrition, "A missing player is rejected before any growth or nutrition change");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void compostGrowthRejectsStaleBlockStates(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        plant(h, pos, 2);
        var crop = AEBlocks.TOMATO_CROP.get();
        var stale = h.getLevel().getBlockState(pos);
        var newer = stale.setValue(AgriCropBlock.AGE, 5);
        h.getLevel().setBlock(pos, newer, 2);
        h.assertFalse(crop.boostGrowth(h.getLevel(), pos, stale), "An old snapshot cannot report successful growth");
        h.assertTrue(h.getLevel().getBlockState(pos) == newer, "Stale growth cannot replace the current age");
        h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        h.assertFalse(crop.boostGrowth(h.getLevel(), pos, stale), "Growth cannot restore a crop that was removed");
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE), "Replacement blocks remain intact");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void compostStillGrowsCropsAndStopsAtMaturity(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var crop = plant(h, pos, 4);
        var genes = new CropGenome(83, 76, 91);
        crop.setGenome(genes);
        int nutrition = crop.soilNutrition();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var compost = new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 3);
        player.setItemInHand(InteractionHand.OFF_HAND, compost);
        compost.useOn(context(player, pos));
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(AgriCropBlock.AGE) == 6
                && compost.getCount() == 2 && crop.soilNutrition() == nutrition + 25, "One use grows two stages and applies nutrition once");
        compost.useOn(context(player, pos));
        int matureNutrition = crop.soilNutrition();
        compost.useOn(context(player, pos));
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(AgriCropBlock.AGE) == 7
                && compost.getCount() == 1 && crop.soilNutrition() == matureNutrition
                && crop.genome().equals(genes), "Mature crops cost nothing and retain their genetics");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void clippersRespectBuildPermissions(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var crop = plant(h, pos, 7);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        var clippers = new ItemStack(AEItems.GENETIC_CLIPPERS.get());
        player.setItemInHand(InteractionHand.OFF_HAND, clippers);
        clippers.getItem().useOn(context(player, pos));
        h.assertTrue(!crop.hasBeenClipped() && player.getInventory().countItem(AEItems.GENETIC_SAMPLE.get()) == 0,
                "Clipping a protected crop cannot consume its one sample");
        player.getAbilities().mayBuild = true;
        clippers.useOn(context(player, pos));
        h.assertTrue(crop.hasBeenClipped() && player.getInventory().countItem(AEItems.GENETIC_SAMPLE.get()) == 1,
                "An authorized player can still collect the unused sample");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullCreativeInventoryReceivesGeneticSampleOnce(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var crop = plant(h, pos, 7);
        var genes = new CropGenome(88, 79, 91);
        crop.setGenome(genes);
        crop.setRareTrait(RareTrait.VIGOROUS);
        var player = h.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        player.setPos(Vec3.atCenterOf(pos));
        fillInventory(player);
        var clippers = new ItemStack(AEItems.GENETIC_CLIPPERS.get());
        player.setItemInHand(InteractionHand.OFF_HAND, clippers);
        clippers.useOn(context(player, pos));
        clippers.useOn(context(player, pos));
        var samples = dropped(h, pos).stream().filter(s -> s.is(AEItems.GENETIC_SAMPLE.get())).toList();
        h.assertTrue(samples.stream().mapToInt(ItemStack::getCount).sum() == 1 && crop.hasBeenClipped(), "A full Creative inventory drops one clipping instead of discarding it");
        h.assertTrue(GeneticSampleItem.genome(samples.getFirst()).equals(genes)
                && GeneticSampleItem.rareTrait(samples.getFirst()) == RareTrait.VIGOROUS, "The dropped sample keeps the crop's genetics");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullCreativeInventoryReceivesSoilSpecimenOnce(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
        var player = h.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        player.setPos(Vec3.atCenterOf(pos));
        fillInventory(player);
        var sampler = new ItemStack(AEItems.SOIL_SAMPLE.get());
        player.setItemInHand(InteractionHand.OFF_HAND, sampler);
        sampler.useOn(context(player, pos));
        sampler.useOn(context(player, pos));
        var samples = dropped(h, pos).stream().filter(s -> s.is(AEItems.SOIL_SPECIMEN.get())).toList();
        h.assertTrue(samples.stream().mapToInt(ItemStack::getCount).sum() == 1, "A collected plot yields one physical specimen even with a full Creative inventory");
        h.assertTrue(SoilSpecimenItem.kind(samples.getFirst()).equals("dirt") && SoilResearch.analyze(sampler, samples.getFirst()),
                "The dropped specimen still matches the sampler's collection credit");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void journalAllowsReadingButNotDeniedBreeding(GameTestHelper h) {
        var first = h.absolutePos(new BlockPos(3, 220, 4));
        var second = first.east(2);
        var a = plant(h, first, 7);
        var b = plant(h, second, 7);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        var book = new ItemStack(AEItems.BREEDING_JOURNAL.get());
        player.setItemInHand(InteractionHand.OFF_HAND, book);
        book.getItem().useOn(context(player, first));
        h.assertTrue(!book.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty(), "Visitors can still record crop discoveries");
        cross(player, first, second);
        h.assertTrue(!a.hasCrossed() && !b.hasCrossed() && player.getInventory().countItem(AEItems.TOMATO_SEED.get()) == 0,
                "Denied breeding leaves both parents unused");
        player.getAbilities().mayBuild = true;
        book.useOn(context(player, second));
        h.assertTrue(a.hasCrossed() && b.hasCrossed() && player.getInventory().countItem(AEItems.TOMATO_SEED.get()) == 1,
                "A rejected cross keeps the selected parent for an authorized retry");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void journalChecksPermissionAtBothParents(GameTestHelper h) {
        var first = h.absolutePos(new BlockPos(3, 220, 4));
        var second = first.east(2);
        var a = plant(h, first, 7);
        var b = plant(h, second, 7);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var book = new ItemStack(AEItems.BREEDING_JOURNAL.get());
        player.setItemInHand(InteractionHand.OFF_HAND, book);
        player.setShiftKeyDown(true);
        book.useOn(context(player, first));
        player.setShiftKeyDown(false);
        var border = h.getLevel().getWorldBorder();
        double oldX = border.getCenterX(), oldZ = border.getCenterZ(), oldSize = border.getSize();
        try {
            border.setCenter(second.getX() + .5, second.getZ() + .5);
            border.setSize(2);
            h.assertTrue(h.getLevel().mayInteract(player, second) && !h.getLevel().mayInteract(player, first), "Only the selected parent is protected");
            book.useOn(context(player, second));
            h.assertTrue(!a.hasCrossed() && !b.hasCrossed() && player.getInventory().countItem(AEItems.TOMATO_SEED.get()) == 0,
                    "The clicked parent's permission cannot authorize the other parent");
        } finally { border.setCenter(oldX, oldZ); border.setSize(oldSize); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullCreativeInventoryReceivesFieldOffspringOnce(GameTestHelper h) {
        var first = h.absolutePos(new BlockPos(3, 220, 4));
        var second = first.east(2);
        var a = plant(h, first, 7);
        var b = plant(h, second, 7);
        var player = h.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        player.setPos(Vec3.atCenterOf(second));
        fillInventory(player);
        var book = new ItemStack(AEItems.BREEDING_JOURNAL.get());
        player.setItemInHand(InteractionHand.OFF_HAND, book);
        cross(player, first, second);
        cross(player, first, second);
        h.assertTrue(dropped(h, second).stream().filter(s -> s.is(AEItems.TOMATO_SEED.get())).mapToInt(ItemStack::getCount).sum() == 1
                && a.hasCrossed() && b.hasCrossed(), "A full Creative inventory drops one bred seed and cannot breed the same parents twice");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sameSpeciesFieldCrossRetainsExistingHybridAncestry(GameTestHelper h) {
        var lineage = new HybridLineageStorage.Lineage(CropSpecies.LETTUCE, CropSpecies.PEPPER);
        for (int example = 0; example < 3; example++) {
            var first = h.absolutePos(new BlockPos(3, 210 + example * 3, 4));
            var second = first.east(2);
            var a = plant(h, first, 7);
            var b = plant(h, second, 7);
            (example == 1 ? b : a).setLineage(lineage);
            if (example == 2) { a.setVariety("First line"); b.setVariety("Second line"); }
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AEItems.BREEDING_JOURNAL.get()));
            cross(player, first, second);
            var child = player.getInventory().items.stream().filter(s -> s.is(AEItems.TOMATO_SEED.get())).findFirst().orElse(ItemStack.EMPTY);
            h.assertTrue(lineage.equals(HybridLineageStorage.fromSeed(child)), "The first available ancestry survives a same-species field cross");
            h.assertTrue(SeedVariety.name(child).isEmpty(), "Different or unnamed varieties do not acquire a shared name");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fieldCrossCommitsParentsBeforeDeliveringOffspring(GameTestHelper h) {
        var first = h.absolutePos(new BlockPos(3, 220, 4));
        var second = first.east(2);
        var a = plant(h, first, 7);
        var b = plant(h, second, 7);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(second));
        fillInventory(player);
        var book = new ItemStack(AEItems.BREEDING_JOURNAL.get());
        player.setItemInHand(InteractionHand.OFF_HAND, book);
        boolean[] observed = {false}, committed = {false};
        java.util.function.Consumer<EntityJoinLevelEvent> observe = event -> {
            if (!(event.getEntity() instanceof ItemEntity item) || !item.getItem().is(AEItems.TOMATO_SEED.get())) return;
            observed[0] = true;
            committed[0] = a.hasCrossed() && b.hasCrossed()
                    && !book.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("AgriSelectedParent");
        };
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(observe);
        try { cross(player, first, second); }
        finally { bus.unregister(observe); }
        h.assertTrue(observed[0] && committed[0], "Item-delivery callbacks see consumed parent rights and a cleared selection");
        h.succeed();
    }
}
