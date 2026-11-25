package com.agrievolution.forge;

import com.agrievolution.platform.*;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraft.core.component.DataComponents;
import net.minecraftforge.common.FarmlandWaterManager;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.ticket.SimpleTicket;
import net.minecraftforge.items.ItemHandlerHelper;

public final class ForgeGameHooks implements GameHooks {
    @Override
    public CompoundTag persistentData(Entity entity) {
        return entity.getPersistentData();
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData) {
        player.openMenu(provider, extraData::accept);
    }

    @Override
    public FoodProperties food(ItemStack stack, Entity consumer) {
        return stack.get(DataComponents.FOOD);
    }

    @Override
    public boolean hasCraftingRemainder(ItemStack stack) {
        return stack.hasCraftingRemainingItem();
    }

    @Override
    public ItemOutput itemOutput(ServerLevel level, BlockPos pos, Direction face) {
        var entity = level.getBlockEntity(pos);
        var handler = entity == null ? null : entity.getCapability(ForgeCapabilities.ITEM_HANDLER, face).orElse(null);
        if (handler == null) return null;
        return new ItemOutput() {
            @Override public int slots() { return handler.getSlots(); }
            @Override public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
                return handler.insertItem(slot, stack, simulate);
            }
            @Override public ItemStack insertStacked(ItemStack stack) {
                return ItemHandlerHelper.insertItemStacked(handler, stack, false);
            }
        };
    }

    @Override
    public void invalidateTransfers(Level level, BlockPos pos) {
        // Port handlers resolve the loaded core on each call.
    }

    @Override
    public WaterTicket waterTicket(ServerLevel level, AABB area) {
        return wrap(FarmlandWaterManager.addAABBTicket(level, area));
    }

    @Override
    public WaterTicket waterTicket(ServerLevel level, Predicate<Vec3> covers, ChunkPos origin, ChunkPos[] additional) {
        return wrap(FarmlandWaterManager.addCustomTicket(level, new SimpleTicket<Vec3>() {
            @Override public boolean matches(Vec3 point) { return covers.test(point); }
        }, origin, additional));
    }

    private static WaterTicket wrap(SimpleTicket<?> ticket) {
        return new WaterTicket() {
            @Override public boolean isValid() { return ticket.isValid(); }
            @Override public void invalidate() { ticket.invalidate(); }
        };
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, BlockPos neighbor) {
        state.onNeighborChange(level, pos, neighbor);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, Level level, BlockPos pos, Direction side) {
        return state.shouldCheckWeakPower(level, pos, side);
    }

    @Override
    public boolean receivesWeakChanges(BlockState state, Level level, BlockPos pos) {
        return state.getWeakChanges(level, pos);
    }

    @Override
    public BlockState tillingState(BlockState state, UseOnContext context) {
        return state.getToolModifiedState(context, ToolActions.HOE_TILL, false);
    }

    @Override
    public boolean capturingPlacement(Level level) {
        return level.captureBlockSnapshots;
    }

    @Override
    public BlockEntity createReservoirPort(BlockPos pos, BlockState state) {
        for (Direction face : Direction.values()) {
            if (com.agrievolution.content.WaterReservoirBlock.port(state, face)) {
                return new ForgeReservoirPort(pos, state);
            }
        }
        return null;
    }

    @Override
    public void onBlockEntityLoad(BlockEntity entity) {
        entity.requestModelDataUpdate();
    }
}
