package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Concentrated compost that advances an Agri Evolution crop by up to two growth stages. */
public final class NutrientCompostItem extends Item {
    public NutrientCompostItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AgriCropBlock crop)) return InteractionResult.PASS;
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            if (!crop.boostGrowth(level, pos, state)) return InteractionResult.PASS;
            if (level.getBlockEntity(pos) instanceof AECropBlockEntity planted) planted.addNutrition(25);
            if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            level.levelEvent(2005, pos, 0);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
