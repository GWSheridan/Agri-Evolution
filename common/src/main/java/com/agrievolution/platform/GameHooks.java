package com.agrievolution.platform;

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

public interface GameHooks {
    String PLAYER_PERSISTED = "PlayerPersisted";

    CompoundTag persistentData(Entity entity);

    void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData);

    FoodProperties food(ItemStack stack, Entity consumer);

    boolean hasCraftingRemainder(ItemStack stack);

    ItemOutput itemOutput(ServerLevel level, BlockPos pos, Direction face);

    void invalidateTransfers(Level level, BlockPos pos);

    WaterTicket waterTicket(ServerLevel level, AABB area);

    WaterTicket waterTicket(ServerLevel level, Predicate<Vec3> covers, ChunkPos origin, ChunkPos[] additional);

    void neighborChanged(BlockState state, Level level, BlockPos pos, BlockPos neighbor);

    default boolean shouldCheckWeakPower(BlockState state, Level level, BlockPos pos, Direction side) {
        return state.isRedstoneConductor(level, pos);
    }

    default boolean receivesWeakChanges(BlockState state, Level level, BlockPos pos) {
        return state.getBlock() instanceof net.minecraft.world.level.block.ComparatorBlock;
    }

    default BlockEntity createReservoirPort(BlockPos pos, BlockState state) {
        return null;
    }

    BlockState tillingState(BlockState state, UseOnContext context);

    boolean capturingPlacement(Level level);

    void onBlockEntityLoad(BlockEntity entity);
}
