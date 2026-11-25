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
        var held = context.getItemInHand();
        var before = held.copy();
        if (held.getItem() != this || !WorkstationAccess.mayCompleteItemUse(level, player, context.getHand(), held, before)
                || !(level.getBlockEntity(pos) instanceof AECropBlockEntity planted)
                || planted.getLevel() != level || !WorkstationAccess.isCurrent(planted)
                || !WorkstationAccess.mayUse(level, pos, player, context.getClickedFace(), held)
                || !WorkstationAccess.mayCompleteItemUse(level, player, context.getHand(), held, before)
                || planted.getLevel() != level || !WorkstationAccess.isCurrent(planted)
                || level.getBlockState(pos) != state) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            if (!crop.boostGrowth(level, pos, state)) return InteractionResult.PASS;
            if (planted.getLevel() == level && WorkstationAccess.isCurrent(planted)) planted.addNutrition(25);
            if (!player.getAbilities().instabuild) held.shrink(1);
            level.levelEvent(2005, pos, 0);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
