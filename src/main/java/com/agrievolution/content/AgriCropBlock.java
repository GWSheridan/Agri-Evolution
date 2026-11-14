package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import com.agrievolution.farming.FarmEnvironment;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** All registered species share the previously working 8-stage growth/genetics system. */
public final class AgriCropBlock extends CropBlock implements EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty SUPPORT_TIER = net.minecraft.world.level.block.state.properties.IntegerProperty.create("support_tier",0,3);
    public static final BooleanProperty SUPPORTED = BooleanProperty.create("supported");
    public static final BooleanProperty WEEDY = BooleanProperty.create("weedy");
    private final CropSpecies species;
    private final Supplier<? extends ItemLike> seedSupplier;

    public AgriCropBlock(Properties properties, CropSpecies species, Supplier<? extends ItemLike> seedSupplier) {
        super(properties);
        this.species = species;
        this.seedSupplier = seedSupplier;
        registerDefaultState(defaultBlockState().setValue(SUPPORTED, false).setValue(SUPPORT_TIER,0).setValue(WEEDY, false));
    }

    public CropSpecies species() { return species; }

    @Override
    protected boolean mayPlaceOn(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof net.minecraft.world.level.block.FarmBlock
                || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SUPPORTED, SUPPORT_TIER, WEEDY);
    }

    @Override
    protected ItemLike getBaseSeedId() { return seedSupplier.get(); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AECropBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return level.isClientSide||type!=AEBlockEntities.AGRI_CROP.get()?null:(world,pos,current,entity)->AECropBlockEntity.tickSupport(world,pos,current,(AECropBlockEntity)entity);
    }

    public boolean boostGrowth(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || !state.hasProperty(AGE)) return false;
        int age = state.getValue(AGE);
        if (age >= getMaxAge()) return false;
        level.setBlock(pos, state.setValue(AGE, Math.min(getMaxAge(), age + 2)), 2);
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Rare visible weeds coexist with the crop; they never replace or destroy it.
        int weedChance=CropSupportItem.weedChance(CropSupportItem.tier(state));
        if(weedChance==0&&state.getValue(WEEDY)){state=state.setValue(WEEDY,false);level.setBlock(pos,state,3);}
        if (weedChance>0 && !state.getValue(WEEDY) && !isMaxAge(state) && random.nextInt(weedChance) == 0) {
            state = state.setValue(WEEDY, true);
            level.setBlock(pos, state, 3);
        }
        // Weeds halve natural growth opportunities; raking restores the original rate.
        if (state.getValue(WEEDY) && random.nextBoolean()) return;
        super.randomTick(state, level, pos, random);
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() != this || current.getValue(AGE) >= getMaxAge()
                || level.getRawBrightness(pos, 0) < 9) return;
        if (level.getBlockEntity(pos) instanceof AECropBlockEntity plant) {
            boolean irrigated = FarmEnvironment.irrigated(level, pos.below());
            int chance = FarmEnvironment.growthChance(plant.genome().growth(), plant.soilNutrition(),
                    FarmEnvironment.moisture(level, pos.below()), CropSupportItem.tier(current), irrigated);
            if (plant.rareTrait() == RareTrait.VIGOROUS) chance += 18;
            // Soil research bonuses stack additively with genes and irrigation, never
            // multiply resource catalysts; higher soils also conserve nutrition.
            int soilTier = com.agrievolution.farming.SoilResearch.soilTier(level.getBlockState(pos.below()));
            chance = Math.min(195, chance + soilTier * 20);
            // Catalyst blocks accelerate only the matching resource crop. No resource
            // catalyst affects other plants; world/light/soil checks still apply.
            float multiplier = com.agrievolution.farming.ResourceCropRules.growthMultiplier(species, level, pos);
            // Strong visible multipliers: x2, x4, x8, x16, x32 for Tiers I-V.
            // Only AgriCropBlock random growth is affected; retain resource catalysts.
            float effectiveMultiplier = multiplier * (1 << soilTier);
            int attempts = Math.min(32, (int) effectiveMultiplier);
            if (attempts < 32 && random.nextFloat() < effectiveMultiplier - attempts) attempts++;
            for (int i = 0; i < attempts; i++) {
                current = level.getBlockState(pos);
                if (current.getBlock() != this || current.getValue(AGE) >= getMaxAge()) break;
                if (random.nextInt(200) < chance) {
                    level.setBlock(pos, current.setValue(AGE, current.getValue(AGE) + 1), 2);
                    if (random.nextInt(3 + soilTier * 2) == 0
                            && (plant.rareTrait() != RareTrait.TENACIOUS || random.nextInt(3) == 0))
                        plant.addNutrition(-1);
                }
            }
        }
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        var drops=new java.util.ArrayList<>(super.getDrops(state,params));
        int tier=CropSupportItem.tier(state);
        if(tier>0){
            var entity=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
            var support=entity instanceof AECropBlockEntity plant?plant.recoverSupport():new ItemStack(CropSupportItem.itemFor(tier));
            if(!support.isEmpty())drops.add(support);
        }
        return drops;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        CropGenome genome = blockEntity instanceof AECropBlockEntity plant
                ? plant.genome() : CropGenome.wild();
        RareTrait rare = blockEntity instanceof AECropBlockEntity plant
                ? plant.rareTrait() : RareTrait.NONE;
        HybridLineageStorage.Lineage lineage = blockEntity instanceof AECropBlockEntity plant
                ? plant.lineage() : null;
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (level.isClientSide || player.getAbilities().instabuild
                || state.getValue(AGE) < getMaxAge()) return;

        ItemStack seed = new ItemStack(AEItems.seedFor(species));
        GenomeStorage.putOnSeed(seed, genome);
        RareTraitStorage.putOnSeed(seed, rare);
        if (lineage != null) HybridLineageStorage.putOnSeed(seed, lineage);
        Block.popResource(level, pos, seed);
        // One extra identical seed may drop; growth/yield/resilience, rare trait
        // and hybrid lineage remain on it. Do not touch other mods' loot tables.
        if (level.random.nextInt(100) <
                com.agrievolution.farming.ExtraSeedRules.chancePercent(
                        genome.yield(), rare == RareTrait.ABUNDANT)) {
            Block.popResource(level, pos, seed.copy());
        }

        boolean bonusHarvest = genome.yield() > 25 && level.random.nextInt(100) < genome.yield();
        if (bonusHarvest || rare == RareTrait.ABUNDANT) {
            int amount = (bonusHarvest ? 1 + genome.yield() / 50 : 0)
                    + (rare == RareTrait.ABUNDANT ? 1 : 0);
            Block.popResource(level, pos, new ItemStack(switch (species) {
                case WILD -> Items.WHEAT;
                case CRIMSON_CLOVER -> Items.RED_DYE;
                case AZURE_FLAX -> Items.BLUE_DYE;
                case VIOLET_STARFLOWER -> Items.PURPLE_DYE;
                case AMBER_MARIGOLD -> Items.ORANGE_DYE;
                case SILVER_THISTLE -> Items.LIGHT_GRAY_DYE;
                case SUNFIRE_LILY -> Items.GLOW_BERRIES;
                case MOON_ORCHID -> Items.AMETHYST_SHARD;
                case ECLIPSE_LOTUS -> Items.ENDER_PEARL;
                case TWILIGHT_SAGE -> Items.GLOWSTONE_DUST;
                case DIRT -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case WOOD -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case STONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case COAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case COPPER -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case IRON -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case REDSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case GOLD -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case LAPIS -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case DIAMOND -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case EMERALD -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case QUARTZ -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case GRAVEL -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case SAND -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case CLAY -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case FLINT -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case GLOWSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case OBSIDIAN -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case NETHERRACK -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case SOUL_SAND -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case MAGMA_CREAM -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case NETHER_WART -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case BLAZE_ROD -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case END_STONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case CHORUS_FRUIT -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case ENDER_PEARL -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case SHULKER_SHELL -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case ANCIENT_DEBRIS -> com.agrievolution.farming.ResourceCropRules.produce(species);
                case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case OAK_SAPLING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPRUCE_SAPLING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BIRCH_SAPLING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DARK_OAK_SAPLING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TUBE_CORAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BRAIN_CORAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BUBBLE_CORAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case FIRE_CORAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case COAST_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TIDE_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SUGAR_CANE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SWEET_BERRIES -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SEAGRASS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HANGING_ROOTS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TORCHFLOWER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PITCHER_PLANT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPORE_BLOSSOM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BIG_DRIPLEAF -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TRIAL_KEY -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case OMINOUS_TRIAL_KEY -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case NETHERITE_UPGRADE_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DISC_FRAGMENT_5 -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GRASS_BLOCK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PODZOL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MYCELIUM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DEAD_BUSH -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            
            case MUD_BRICKS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DEEPSLATE_BRICKS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CALIBRATED_SCULK_SENSOR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case END_CRYSTAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TOTEM_OF_UNDYING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ENCHANTED_GOLDEN_APPLE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case JUNGLE_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case COARSE_DIRT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ROOTED_DIRT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_SANDSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PRISMARINE_BRICKS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            case WARPED_WART_BLOCK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_NETHER_BRICKS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SCULK_SENSOR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SCULK_SHRIEKER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CONDUIT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RESPAWN_ANCHOR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case STICK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DRIED_KELP -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GRAY_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LIGHT_GRAY_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            
            
            
            case SKELETON_SKULL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WITHER_SKELETON_SKULL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CREEPER_HEAD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ZOMBIE_HEAD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WHITE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BLACK_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ORANGE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PINK_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PURPLE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MAGENTA_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LIGHT_BLUE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LIME_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HEART_OF_THE_SEA -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case NETHER_STAR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DRAGON_HEAD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HEAVY_CORE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GREEN_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case YELLOW_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BROWN_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HONEY_BOTTLE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GLOW_BERRIES -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BLUE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CYAN_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case FIRE_CHARGE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WIND_CHARGE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DRAGON_BREATH -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case EXPERIENCE_BOTTLE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SUGAR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case COCOA_BEANS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case EGG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ROTTEN_FLESH -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RABBIT_HIDE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RABBIT_FOOT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ARMADILLO_SCUTE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case FERMENTED_SPIDER_EYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WITHER_ROSE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CHORUS_FLOWER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CRIMSON_FUNGUS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WARPED_FUNGUS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPRUCE_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BIRCH_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ACACIA_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DARK_OAK_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MANGROVE_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CHERRY_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SEA_PICKLE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GLOW_LICHEN -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CRIMSON_STEM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WARPED_STEM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case VERDANT_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PEARLESCENT_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CHARCOAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GLASS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BROWN_MUSHROOM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_MUSHROOM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            
            case MOSSY_COBBLESTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LILY_PAD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DARK_PRISMARINE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GILDED_BLACKSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SCULK_CATALYST -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case OCHRE_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_SAND -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SANDSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CACTUS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case VINE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PACKED_MUD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BRICK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ICE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PACKED_ICE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BLUE_ICE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            case NETHER_BRICK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ENDER_EYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GRANITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DIORITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ANDESITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MUD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TUFF -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DRIPSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DEEPSLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SOUL_SOIL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SEA_LANTERN -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SHROOMLIGHT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case END_ROD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SCULK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PAPER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case KELP -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BAMBOO -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BLACKSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BASALT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PURPUR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TURTLE_SCUTE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPONGE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SNOWBALL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MOSS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CALCITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TERRACOTTA -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPIDER_EYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GLOW_INK_SAC -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CRYING_OBSIDIAN -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case NAUTILUS_SHELL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case FEATHER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case INK_SAC -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HONEYCOMB -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SLIME_BALL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GUNPOWDER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PRISMARINE_CRYSTALS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PHANTOM_MEMBRANE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ECHO_SHARD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case STRING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WOOL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LEATHER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case AMETHYST -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PRISMARINE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GHAST_TEAR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BREEZE_ROD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BASIL -> AEItems.BASIL.get();
            case MINT -> AEItems.MINT.get();
            case ROSEMARY -> AEItems.ROSEMARY.get();
            case THYME -> AEItems.THYME.get();
            case PARSLEY -> AEItems.PARSLEY.get();
            case CORIANDER -> AEItems.CORIANDER.get();
            case GINGER -> AEItems.GINGER.get();
            case TURMERIC -> AEItems.TURMERIC.get();
            case KALE -> AEItems.KALE.get();
            case PAK_CHOI -> AEItems.PAK_CHOI.get();
            case BRUSSELS_SPROUTS -> AEItems.BRUSSELS_SPROUTS.get();
            case SWEDE -> AEItems.SWEDE.get();
            case CELERIAC -> AEItems.CELERIAC.get();
            case LENTIL -> AEItems.LENTIL.get();
            case CHICKPEA -> AEItems.CHICKPEA.get();
            case BARLEY -> AEItems.BARLEY.get();
            case BUCKWHEAT -> AEItems.BUCKWHEAT.get();
            case RHUBARB -> AEItems.RHUBARB.get();
            case GRAPE -> AEItems.GRAPE.get();
            case CANTALOUPE -> AEItems.CANTALOUPE.get();
            case SPINACH -> AEItems.SPINACH.get();
            case CAULIFLOWER -> AEItems.CAULIFLOWER.get();
            case ARTICHOKE -> AEItems.ARTICHOKE.get();
            case CHILLI -> AEItems.CHILLI.get();
            case SPRING_ONION -> AEItems.SPRING_ONION.get();
            case FENNEL -> AEItems.FENNEL.get();
            case DILL -> AEItems.DILL.get();
            case PEANUT -> AEItems.PEANUT.get();
            case SESAME -> AEItems.SESAME.get();
            case OATS -> AEItems.OATS.get();
            case GOOSEBERRY -> AEItems.GOOSEBERRY.get();
            case BLACKCURRANT -> AEItems.BLACKCURRANT.get();
            case PUMPKIN -> AEItems.PUMPKIN.get();
            case BUTTERNUT_SQUASH -> AEItems.BUTTERNUT_SQUASH.get();
            case COURGETTE -> AEItems.COURGETTE.get();
            case TURNIP -> AEItems.TURNIP.get();
            case PARSNIP -> AEItems.PARSNIP.get();
            case LEEK -> AEItems.LEEK.get();
            case CELERY -> AEItems.CELERY.get();
            case ASPARAGUS -> AEItems.ASPARAGUS.get();
            case BLUEBERRY -> AEItems.BLUEBERRY.get();
            case RASPBERRY -> AEItems.RASPBERRY.get();
            case BLACKBERRY -> AEItems.BLACKBERRY.get();
            case CRANBERRY -> AEItems.CRANBERRY.get();
            case CORN -> AEItems.CORN.get();
            case PEAS -> AEItems.PEAS.get();
            case BEANS -> AEItems.BEANS.get();
            case BROCCOLI -> AEItems.BROCCOLI.get();
            case CABBAGE -> AEItems.CABBAGE.get();
            case RADISH -> AEItems.RADISH.get();
            case AUBERGINE -> AEItems.AUBERGINE.get();
            case SWEET_POTATO -> AEItems.SWEET_POTATO.get();
            case TOMATO -> AEItems.TOMATO.get();
            case LETTUCE -> AEItems.LETTUCE.get();
            case CUCUMBER -> AEItems.CUCUMBER.get();
            case ONION -> AEItems.ONION.get();
            case GARLIC -> AEItems.GARLIC.get();
            case PEPPER -> AEItems.PEPPER.get();
            case STRAWBERRY -> AEItems.STRAWBERRY.get();
            case RICE -> AEItems.RICE.get();
            }, amount));
        }
    }
}
