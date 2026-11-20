package com.agrievolution.content;

import java.util.List;
import com.agrievolution.farming.SoilResearch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class GardenToolItem extends Item {
    private final boolean trowel;
    private final int nutrition;
    public GardenToolItem(Properties properties, boolean trowel, int nutrition) {
        super(properties);
        this.trowel = trowel;
        this.nutrition = nutrition;
    }
    @Override public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility ability) {
        return trowel && ability == net.neoforged.neoforge.common.ItemAbilities.HOE_TILL;
    }
    private static boolean plantable(ItemStack stack) {
        return SeedBagItem.accepts(stack) || (!stack.isEmpty() && stack.getItem() instanceof BlockItem item
                && CropWeeds.isHost(item.getBlock().defaultBlockState()));
    }
    public static boolean recentlyUsed(ItemStack stack, Level level) {
        return level != null && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getLong("AgriTrowelActiveUntil") > level.getGameTime();
    }

    private boolean areaWeeding(UseOnContext context) {
        return !trowel && this == AEItems.RAKE_IRON.get()
                && context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
    }

    private boolean hasPlantingInput(UseOnContext context) {
        Player player = context.getPlayer();
        return trowel && player != null && context.getHand() == InteractionHand.MAIN_HAND
                && context.getClickedFace() == Direction.UP && plantable(player.getOffhandItem());
    }

    private static boolean canPlantPatch(BlockState soil, ItemStack seed) {
        // The caller has already checked the same seed compatibility used by single planting.
        return SoilResearch.isSoil(soil) && (seed.getItem() instanceof SpeciesSeedItem || seed.getItem() instanceof BlockItem);
    }

    private boolean currentTool(UseOnContext context, ItemStack tool, ItemStack before, boolean crouching) {
        var player = context.getPlayer();
        return tool.getItem() == this && WorkstationAccess.mayCompleteItemUse(context.getLevel(), player, context.getHand(), tool, before)
                && player.isShiftKeyDown() == crouching && !player.getCooldowns().isOnCooldown(this);
    }

    private static boolean sameTarget(Level level, BlockPos pos, BlockState state, BlockEntity entity) {
        return level.hasChunkAt(pos) && level.getBlockState(pos) == state && level.getBlockEntity(pos) == entity
                && (entity == null || entity.getLevel() == level && WorkstationAccess.isCurrent(entity));
    }

    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Plant before the trowel enters its own block-snapshot transaction. The seed
        // then gets one normal placement transaction, including cancellation/refund.
        if (hasPlantingInput(context)) {
            var player = context.getPlayer();
            var level = context.getLevel();
            var soil = context.getClickedPos();
            if (player.isShiftKeyDown() ? canPlantPatch(level.getBlockState(soil), player.getOffhandItem()) : level.isEmptyBlock(soil.above())) {
                return useOn(context);
            }
        }
        // Clear weeds before a mod's right-click harvesting action can consume the click.
        if (areaWeeding(context)) return useOn(context);
        if (!trowel && (CropWeeds.hasWeeds(context.getLevel(), context.getClickedPos())
                || CropWeeds.hasWeeds(context.getLevel(), context.getClickedPos().above()))) return useOn(context);
        return InteractionResult.PASS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        var tool = context.getItemInHand();
        var beforeTool = tool.copy();
        boolean crouching = player != null && player.isShiftKeyDown();
        if (!currentTool(context, tool, beforeTool, crouching) || !level.hasChunkAt(clicked)) return InteractionResult.FAIL;
        BlockState hit = level.getBlockState(clicked);
        BlockState above = level.getBlockState(clicked.above());
        var clickedEntity = level.getBlockEntity(clicked);
        var aboveEntity = level.getBlockEntity(clicked.above());
        if (!WorkstationAccess.mayUse(level, clicked, player, context.getClickedFace(), tool)
                || !currentTool(context, tool, beforeTool, crouching)
                || !sameTarget(level, clicked, hit, clickedEntity)
                || !sameTarget(level, clicked.above(), above, aboveEntity)) return InteractionResult.FAIL;
        if (areaWeeding(context)) return weedPatch(context, player, level, clicked, beforeTool, crouching);
        boolean plantingInput = hasPlantingInput(context);
        if (plantingInput && player.isShiftKeyDown() && canPlantPatch(hit, player.getOffhandItem())) {
            return plantPatch(context, player, level, clicked);
        }
        if (trowel && player.isShiftKeyDown() && level.isEmptyBlock(clicked.above())) {
            BlockState tilled = hit.getToolModifiedState(context, net.neoforged.neoforge.common.ItemAbilities.HOE_TILL, false);
            if (tilled != null && !tilled.equals(hit)) {
                if (!currentTool(context, tool, beforeTool, crouching)
                        || !sameTarget(level, clicked, hit, clickedEntity) || !sameTarget(level, clicked.above(), above, aboveEntity)
                        || !WorkstationAccess.mayUse(level, clicked, player, context.getClickedFace(), tool)
                        || !currentTool(context, tool, beforeTool, crouching)
                        || !sameTarget(level, clicked, hit, clickedEntity) || !sameTarget(level, clicked.above(), above, aboveEntity))
                    return InteractionResult.FAIL;
                if (!level.isClientSide && !level.setBlock(clicked, tilled, 3))
                    return InteractionResult.FAIL;
                finishUse(context, clicked.above(), false, beforeTool, crouching);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (plantingInput && !player.isShiftKeyDown()
                && !level.captureBlockSnapshots
                && level.isEmptyBlock(clicked.above()) && level.mayInteract(player, clicked.above())
                && player.mayUseItemAt(clicked.above(), context.getClickedFace(), player.getOffhandItem())) {
            // Let the seed's own placement code preserve genes, soil rules and placement events.
            var seedContext = new UseOnContext(player, InteractionHand.OFF_HAND,
                    new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), context.getClickedFace(), clicked, context.isInside()));
            InteractionResult result = player.getOffhandItem().useOn(seedContext);
            if (result.consumesAction()) {
                if (!level.isClientSide && !level.isEmptyBlock(clicked.above())) {
                    givePlantingBonus(level, clicked.above());
                    finishUse(context, clicked.above(), true, beforeTool, crouching);
                }
                return result;
            }
            return result;
        }
        // Rakes can clear crops on soul sand, custom soils and other non-farmland hosts.
        BlockPos soil = (hit.getBlock() instanceof FarmBlock || CropWeeds.isHost(level.getBlockState(clicked.above()))) ? clicked : clicked.below();
        BlockPos plantPos = soil.above();
        BlockState state = level.getBlockState(plantPos);
        BlockState farmland = level.getBlockState(soil);
        var soilEntity = level.getBlockEntity(soil);
        var plantEntity = level.getBlockEntity(plantPos);
        if (!WorkstationAccess.mayUse(level, soil, player, context.getClickedFace(), tool)
                || !currentTool(context, tool, beforeTool, crouching)
                || !WorkstationAccess.mayUse(level, plantPos, player, context.getClickedFace(), tool)
                || !currentTool(context, tool, beforeTool, crouching)
                || !sameTarget(level, soil, farmland, soilEntity) || !sameTarget(level, plantPos, state, plantEntity)) return InteractionResult.FAIL;
        AECropBlockEntity plant = plantEntity instanceof AECropBlockEntity p ? p : null;
        if (trowel) {
            if (!(state.getBlock() instanceof AgriCropBlock crop) || plant == null) return InteractionResult.PASS;
            if (crop.isMaxAge(state) || plant.hasBeenTended()) {
                if (!level.isClientSide) player.displayClientMessage(Component.literal(
                        crop.isMaxAge(state) ? "This crop is already mature." : "Already tended. Replant to tend again."), true);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                plant.setTended();
                plant.addNutrition(nutrition);
                level.setBlock(plantPos, state.setValue(AgriCropBlock.AGE, crop.getAge(state) + 1), 3);
                player.displayClientMessage(Component.literal("Crop tended: +1 growth stage, +10 nutrition."), true);
            }
        } else {
            boolean weeds = CropWeeds.hasWeeds(level, plantPos);
            boolean feed = plant != null && plant.soilNutrition() < 100;
            boolean hydrate = farmland.getBlock() instanceof FarmBlock && farmland.getValue(FarmBlock.MOISTURE) < 7;
            if (!weeds && !feed && !hydrate) return InteractionResult.PASS;
            if (!level.isClientSide) {
                if (hydrate) level.setBlock(soil, farmland.setValue(FarmBlock.MOISTURE, 7), 3);
                if (plant != null) plant.addNutrition(nutrition);
                if (weeds) {
                    CropWeeds.clear(level, plantPos);
                    collectWeeds(player, 1);
                }
                player.displayClientMessage(Component.literal(weeds ? "Weeds cleared." : "Soil refreshed."), true);
            }
        }
        finishUse(context, plantPos, trowel, beforeTool, crouching);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult plantPatch(UseOnContext context, Player player, Level level, BlockPos center) {
        // Each seed opens and completes its own NeoForge placement transaction.
        if (level.captureBlockSnapshots) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        ItemStack tool = context.getItemInHand();
        Item seedItem = player.getOffhandItem().getItem();
        int planted = 0;
        for (BlockPos candidate : BlockPos.withinManhattan(center, 1, 0, 1)) {
            ItemStack seeds = player.getOffhandItem();
            if (tool.isEmpty() || player.getMainHandItem() != tool || !seeds.is(seedItem)) break;
            BlockPos soil = candidate.immutable();
            BlockPos target = soil.above();
            if (server.isOutsideBuildHeight(target) || !server.hasChunkAt(soil)
                    || !SoilResearch.isSoil(server.getBlockState(soil)) || !server.isEmptyBlock(target)
                    || !server.mayInteract(player, soil) || !server.mayInteract(player, target)
                    || !player.mayUseItemAt(target, Direction.UP, seeds)) continue;

            var hit = new BlockHitResult(Vec3.atBottomCenterOf(target), Direction.UP, soil, false);
            var seedContext = new UseOnContext(player, InteractionHand.OFF_HAND, hit);
            if (!seeds.useOn(seedContext).consumesAction() || server.isEmptyBlock(target)) continue;
            givePlantingBonus(server, target);
            planted++;
            damageTool(context, player);
        }
        if (planted == 0) {
            player.displayClientMessage(Component.literal("No seeds planted in this patch."), true);
        } else {
            if (!tool.isEmpty()) markPlanted(tool, server);
            playUseEffects(server, center, center.above());
            player.getCooldowns().addCooldown(this, 10);
            player.displayClientMessage(Component.literal("Planted " + planted + (planted == 1 ? " crop." : " crops.")), true);
        }
        return InteractionResult.CONSUME;
    }

    private void givePlantingBonus(Level level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        if (state.getBlock() instanceof AgriCropBlock crop && !crop.isMaxAge(state)
                && level.getBlockEntity(target) instanceof AECropBlockEntity plant && !plant.hasBeenTended()) {
            plant.setTended();
            plant.addNutrition(nutrition);
            level.setBlock(target, state.setValue(AgriCropBlock.AGE, crop.getAge(state) + 1), 3);
        }
    }

    private InteractionResult weedPatch(UseOnContext context, Player player, Level level, BlockPos clicked,
                                        ItemStack beforeTool, boolean crouching) {
        BlockState hit = level.getBlockState(clicked);
        BlockPos center = clicked;
        if (!CropWeeds.isHost(hit)) {
            center = clicked.above();
            if (!(hit.getBlock() instanceof FarmBlock) && !CropWeeds.isHost(level.getBlockState(center))) return InteractionResult.PASS;
        }
        var centerState = level.getBlockState(center);
        var centerEntity = level.getBlockEntity(center);
        if (level.isOutsideBuildHeight(center) || !WorkstationAccess.mayUse(level, center, player, context.getClickedFace(), context.getItemInHand())
                || !currentTool(context, context.getItemInHand(), beforeTool, crouching)
                || !sameTarget(level, center, centerState, centerEntity)) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        int cleared = 0;
        ItemStack rake = context.getItemInHand();
        // Start at the clicked plot, then work outwards in case the rake breaks.
        for (BlockPos plot : BlockPos.withinManhattan(center, 1, 0, 1)) {
            var before = rake.copy();
            if (!currentTool(context, rake, before, crouching)) break;
            if (!server.hasChunkAt(plot)) continue;
            var state = server.getBlockState(plot);
            var farmland = server.getBlockState(plot.below());
            var plantEntity = server.getBlockEntity(plot);
            var soilEntity = server.getBlockEntity(plot.below());
            boolean allowed = WorkstationAccess.mayUse(server, plot, player, context.getClickedFace(), rake)
                    && currentTool(context, rake, before, crouching)
                    && WorkstationAccess.mayUse(server, plot.below(), player, context.getClickedFace(), rake);
            if (!currentTool(context, rake, before, crouching)) break;
            if (!allowed || !sameTarget(server, plot, state, plantEntity) || !sameTarget(server, plot.below(), farmland, soilEntity)) continue;
            if (!CropWeeds.isHost(server.getBlockState(plot)) || !CropWeeds.hasWeeds(server, plot)) continue;
            CropWeeds.clear(server, plot);
            cleared++;
            damageTool(context, player);
        }

        if (cleared == 0) {
            player.displayClientMessage(Component.literal("No weeds cleared in this patch."), true);
        } else {
            collectWeeds(player, cleared);
            playUseEffects(server, clicked, center);
            player.getCooldowns().addCooldown(this, 10);
            player.displayClientMessage(Component.literal("Cleared weeds from " + cleared + (cleared == 1 ? " plot." : " plots.")), true);
        }
        return InteractionResult.CONSUME;
    }

    private static void collectWeeds(Player player, int count) {
        if (!player.getAbilities().instabuild) {
            player.getInventory().placeItemBackInInventory(new ItemStack(AEItems.PULLED_WEEDS.get(), count));
        }
    }

    private void finishUse(UseOnContext context, BlockPos plantPos, boolean planted, ItemStack beforeTool, boolean crouching) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!currentTool(context, context.getItemInHand(), beforeTool, crouching)) return;
        if (level instanceof ServerLevel server) {
            if (planted) markPlanted(context.getItemInHand(), level);
            playUseEffects(server, context.getClickedPos(), plantPos);
            damageTool(context, player);
            player.getCooldowns().addCooldown(this, 10);
        }
    }

    private static void markPlanted(ItemStack tool, Level level) {
        CustomData.update(DataComponents.CUSTOM_DATA, tool, tag -> tag.putLong("AgriTrowelActiveUntil", level.getGameTime() + 60));
    }

    private static void playUseEffects(ServerLevel level, BlockPos soundPos, BlockPos plantPos) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, plantPos.getX() + .5, plantPos.getY() + .35,
                plantPos.getZ() + .5, 6, .25, .15, .25, 0);
        level.playSound(null, soundPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, .7f, 1.1f);
    }

    private static void damageTool(UseOnContext context, Player player) {
        context.getItemInHand().hurtAndBreak(1, player, context.getHand() == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(trowel ? "Tend a growing Agri crop once per planting." : "Use on a weedy crop or its soil to clear weeds."));
        if (trowel) {
            lines.add(Component.literal("Sneak-use on dirt to prepare soil."));
            lines.add(Component.literal("Hold seeds offhand; use on soil to plant."));
            lines.add(Component.literal("Crouch-use on farmland with seeds to plant a 3 x 3 patch."));
        }
        if (this == AEItems.RAKE_IRON.get()) lines.add(Component.literal("Crouch-use to weed a 3 x 3 patch. Costs 1 durability per cleared plot."));
        lines.add(Component.literal(trowel ? "+1 growth stage and +10 nutrition; no gene changes."
                : "Restores moisture; adds up to " + nutrition + " crop nutrition."));
    }
}
