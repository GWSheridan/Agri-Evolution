package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class CompostBinBlock extends Block {
    public static final int FULL = 8, READY = 9, FERMENT_TICKS = 200;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, READY);
    private static final TagKey<Item> MATERIAL = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(AgriEvolution.MOD_ID, "compost_bin_materials"));

    public CompostBinBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FILL, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    static int value(ItemStack stack) {
        if (stack.is(AEItems.PULLED_WEEDS.get())) return 2;
        return stack.is(ItemTags.LEAVES) || stack.is(MATERIAL) ? 1 : 0;
    }

    private boolean currentState(Level level, BlockPos pos, BlockState state) {
        return state.is(this) && level.hasChunkAt(pos) && level.getBlockState(pos) == state;
    }

    private static boolean mayEdit(Level level, BlockPos pos, Player player, ItemStack stack, BlockHitResult hit) {
        return WorkstationAccess.mayUse(level, pos, player, hit.getDirection(), stack);
    }

    private boolean currentUse(Level level, BlockPos pos, BlockState state, Player player,
                               InteractionHand hand, ItemStack stack, ItemStack before) {
        return WorkstationAccess.mayCompleteItemUse(level, player, hand, stack, before) && currentState(level, pos, state);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (!currentState(level, pos, state)) return ItemInteractionResult.FAIL;
        if (hand == InteractionHand.MAIN_HAND && stack.isEmpty() && state.getValue(FILL) < READY
                && player != null && value(player.getOffhandItem()) > 0) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        int amount = value(stack);
        if (amount == 0 || state.getValue(FILL) == READY) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var before = stack.copy();
        if (!currentUse(level, pos, state, player, hand, stack, before)
                || !mayEdit(level, pos, player, stack, hit)
                || !currentUse(level, pos, state, player, hand, stack, before)) return ItemInteractionResult.FAIL;
        if (!level.isClientSide) {
            int used = deposit((ServerLevel) level, pos, state, stack, amount);
            stack.consume(used, player);
            var current = level.getBlockState(pos);
            if (current.is(this)) showFill(player, current.getValue(FILL));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int deposit(ServerLevel level, BlockPos pos, BlockState state, ItemStack stack, int amount) {
        int fill = state.getValue(FILL);
        if (fill >= FULL || stack.isEmpty() || amount <= 0 || level.getBlockState(pos) != state) return 0;
        int next = Math.min(FULL, fill + amount);
        if (!level.setBlock(pos, state.setValue(FILL, next), UPDATE_ALL)) return 0;
        if (next == FULL) level.scheduleTick(pos, state.getBlock(), FERMENT_TICKS);
        return 1;
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (currentState(level, pos, state) && state.getValue(FILL) == FULL)
            level.setBlock(pos, state.setValue(FILL, READY), UPDATE_ALL);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!currentState(level, pos, state) || player == null) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            int fill = state.getValue(FILL);
            var held = player.getMainHandItem();
            var before = held.copy();
            if (fill == READY && currentUse(level, pos, state, player, InteractionHand.MAIN_HAND, held, before)
                    && mayEdit(level, pos, player, held, hit)
                    && currentUse(level, pos, state, player, InteractionHand.MAIN_HAND, held, before)
                    && level.setBlock(pos, state.setValue(FILL, 0), UPDATE_ALL)) {
                player.getInventory().placeItemBackInInventory(new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 2));
                player.displayClientMessage(Component.literal("Collected 2 Nutrient Compost"), true);
            } else showFill(player, fill);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void showFill(Player player, int fill) {
        player.displayClientMessage(Component.literal(fill == READY ? "Compost ready"
                : fill == FULL ? "Compost breaking down" : "Compost bin " + fill + " / " + FULL), true);
    }
}
