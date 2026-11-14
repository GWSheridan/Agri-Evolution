package com.agrievolution.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class SeedVaultBlock extends BaseEntityBlock {
    private static final MapCodec<SeedVaultBlock> CODEC = simpleCodec(SeedVaultBlock::new);

    public SeedVaultBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<SeedVaultBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SeedVaultBlockEntity(pos, state); }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof SeedVaultBlockEntity vault) {
            server.openMenu(vault);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof SeedVaultBlockEntity vault) {
            Containers.dropContents(level, pos, vault);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
