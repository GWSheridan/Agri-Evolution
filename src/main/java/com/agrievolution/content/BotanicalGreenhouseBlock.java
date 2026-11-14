package com.agrievolution.content;

import com.mojang.serialization.MapCodec;
import com.agrievolution.farming.BotanicalGreenhouseLayout;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Containers;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Wall-mounted controller for the 7x7, glass-roofed Botanical Greenhouse. */
public final class BotanicalGreenhouseBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty UPGRADED = BooleanProperty.create("upgraded");
    public static final BooleanProperty SMART = BooleanProperty.create("smart");
    public static final MapCodec<BotanicalGreenhouseBlock> CODEC = simpleCodec(BotanicalGreenhouseBlock::new);

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public BotanicalGreenhouseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false).setValue(UPGRADED, false).setValue(SMART, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ACTIVE, UPGRADED, SMART);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BotanicalGreenhouseBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, AEBlockEntities.BOTANICAL_GREENHOUSE.get(),
                BotanicalGreenhouseBlockEntity::serverTick);
    }

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof BotanicalGreenhouseBlockEntity greenhouse) {
            server.openMenu(greenhouse);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        // A client-only hologram; toggling never changes the actual controller state.
        if (player.isShiftKeyDown()) {
            if (level.isClientSide)
                com.agrievolution.client.greenhouse.GreenhouseHologramClient.toggle(pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return open(level, pos, player);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                                         BlockPos pos, Player player, InteractionHand hand,
                                                         BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (level.isClientSide)
                com.agrievolution.client.greenhouse.GreenhouseHologramClient.toggle(pos);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // The existing basic controller must be upgraded before automated farming is available.
        if (held.is(AEItems.GREENHOUSE_AUTOMATION_UPGRADE.get())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BotanicalGreenhouseBlockEntity gh) {
                if (gh.installAutomationUpgrade()) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    player.displayClientMessage(Component.literal("Greenhouse controller upgraded: automated farming unlocked!"), true);
                } else {
                    player.displayClientMessage(Component.literal("Greenhouse automation already installed."), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // Tier 3 requires the earlier automation module, and is never consumed if it cannot apply.
        if (held.is(AEItems.GREENHOUSE_SMART_FARMING_UPGRADE.get())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BotanicalGreenhouseBlockEntity gh) {
                if (!gh.isAutomationUpgraded()) {
                    player.displayClientMessage(Component.literal(
                            "Install the Greenhouse Automation Upgrade first."), true);
                } else if (gh.installSmartFarmingUpgrade()) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    player.displayClientMessage(Component.literal(
                            "Smart Farming unlocked: open the Planting tab to load genetically unique seeds!"), true);
                } else {
                    player.displayClientMessage(Component.literal("Smart Farming already installed."), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // Holding any vanilla or tagged modded door and using it on the controller
        // fits an actual two-block door into the front wall, even after the glass
        // enclosure is complete. Shift + use still toggles the hologram.
        if (held.getItem() instanceof BlockItem doorItem
                && (doorItem.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                    || doorItem.getBlock().defaultBlockState().is(BlockTags.DOORS)
                    || held.is(ItemTags.DOORS))) {
            return installDoor(held, state, level, pos, player, hand);
        }
        // Coal/charcoal refills the optional climate heater. One item is ten minutes.
        if (held.is(Items.COAL) || held.is(Items.CHARCOAL)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BotanicalGreenhouseBlockEntity greenhouse) {
                if (!greenhouse.addHeaterFuel()) {
                    player.displayClientMessage(Component.literal("Heater fuel tank full (120 minutes)."), true);
                    return ItemInteractionResult.FAIL;
                }
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.literal("Heater fueled: +10 minutes."), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BotanicalGreenhouseBlockEntity greenhouse) {
                if (!greenhouse.addWater(1000)) return ItemInteractionResult.FAIL;
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.is(Items.BUCKET)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BotanicalGreenhouseBlockEntity greenhouse) {
                if (!greenhouse.removeWater(1000)) return ItemInteractionResult.FAIL;
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Uses the mod's actual door ItemStack placement logic, not a hardcoded oak-door blockstate.
     *  This preserves modded door orientations, block entities and item consumption. */
    private ItemInteractionResult installDoor(ItemStack held, BlockState state, Level level,
                                              BlockPos controller, Player player, InteractionHand hand) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        Direction facing = state.getValue(FACING);
        if (BotanicalGreenhouseLayout.hasValidDoor(level, controller, facing)) {
            player.displayClientMessage(Component.literal("Greenhouse entrance: a door is already installed."), true);
            return ItemInteractionResult.SUCCESS;
        }
        // Recommended doorway is immediately beside the controller, on the front wall.
        BlockPos lower = BotanicalGreenhouseLayout.at(controller, facing, 1, 0, 0);
        BlockPos upper = lower.above();
        if (!level.hasChunkAt(lower) || !level.hasChunkAt(upper)
                || !level.mayInteract(player, lower) || !level.mayInteract(player, upper)
                || !player.mayUseItemAt(lower, Direction.UP, held)) return ItemInteractionResult.FAIL;
        BlockState oldLower = level.getBlockState(lower);
        BlockState oldUpper = level.getBlockState(upper);
        if (!BotanicalGreenhouseLayout.canReplaceWithDoor(oldLower)
                || !BotanicalGreenhouseLayout.canReplaceWithDoor(oldUpper)) {
            player.displayClientMessage(Component.literal(
                    "Doorway blocked. Clear the two front-wall blocks beside the controller."), true);
            return ItemInteractionResult.FAIL;
        }
        // Remove only glass or air; restore both original states if the modded item cannot place.
        level.setBlock(upper, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(lower, Blocks.AIR.defaultBlockState(), 3);
        BlockPos base = lower.below();
        BlockHitResult syntheticHit = new BlockHitResult(
                new Vec3(lower.getX() + 0.5, lower.getY(), lower.getZ() + 0.5),
                Direction.UP, base, false);
        ItemStack beforePlacement = held.copy();
        InteractionResult placement = held.useOn(new UseOnContext(player, hand, syntheticHit));
        if (!placement.consumesAction()
                || !BotanicalGreenhouseLayout.hasValidDoor(level, controller, facing)) {
            // Undo partially placed door, if any, before restoring the real wall.
            level.setBlock(upper, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(lower, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(lower, oldLower, 3);
            level.setBlock(upper, oldUpper, 3);
            // An unusual modded door item may consume its stack before failing validation.
            player.setItemInHand(hand, beforePlacement);
            player.displayClientMessage(Component.literal(
                    "That door could not be placed here. Check the floor beneath the entrance."), true);
            return ItemInteractionResult.FAIL;
        }
        giveBackGlass(player, level, lower, oldLower);
        giveBackGlass(player, level, upper, oldUpper);
        player.displayClientMessage(Component.literal("Greenhouse entrance installed!"), true);
        if (level.getBlockEntity(controller) instanceof BotanicalGreenhouseBlockEntity greenhouse)
            greenhouse.refresh();
        return ItemInteractionResult.SUCCESS;
    }

    @Override protected void onRemove(BlockState old, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!old.is(replacement.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof BotanicalGreenhouseBlockEntity greenhouse) {
            Containers.dropContents(level, pos, greenhouse);
            Containers.dropContents(level, pos, greenhouse.seedBank());
            if (greenhouse.isSmartFarmingUpgraded())
                Block.popResource(level, pos, new ItemStack(AEItems.GREENHOUSE_SMART_FARMING_UPGRADE.get()));
            // Return the crafted module when an upgraded controller is dismantled.
            if (greenhouse.isAutomationUpgraded())
                Block.popResource(level, pos, new ItemStack(AEItems.GREENHOUSE_AUTOMATION_UPGRADE.get()));
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(old, level, pos, replacement, moved);
    }

    private static void giveBackGlass(Player player, Level level, BlockPos pos, BlockState old) {
        if (old.isAir()) return;
        ItemStack returned = new ItemStack(old.getBlock().asItem());
        if (returned.isEmpty()) return;
        if (!player.getInventory().add(returned)) Block.popResource(level, pos, returned);
    }

}
