package com.agrievolution.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.FarmBlock;

public final class CompostMulchItem extends Item {
    public CompostMulchItem(Properties properties){super(properties);}
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();var player=context.getPlayer();var clicked=context.getClickedPos();
        if(player==null || !level.hasChunkAt(clicked))return InteractionResult.PASS;
        if(!(level.getBlockState(clicked).getBlock() instanceof FarmBlock) && !CropWeeds.isHost(level.getBlockState(clicked)))
            return InteractionResult.PASS;
        BlockPos soil=level.getBlockState(clicked).getBlock() instanceof FarmBlock?clicked:clicked.below();
        if(!level.hasChunkAt(soil) || !(level.getBlockState(soil).getBlock() instanceof FarmBlock))return InteractionResult.PASS;
        var held=context.getItemInHand();var before=held.copy();var ground=level.getBlockState(soil);var crop=level.getBlockState(soil.above());
        if(held.getItem()!=this || !WorkstationAccess.mayUse(level,soil,player,context.getClickedFace(),held)
                || !WorkstationAccess.mayUse(level,soil.above(),player,context.getClickedFace(),held)
                || !WorkstationAccess.mayCompleteItemUse(level,player,context.getHand(),held,before)
                || level.getBlockState(soil)!=ground || level.getBlockState(soil.above())!=crop)return InteractionResult.FAIL;
        if(level instanceof ServerLevel server) {
            if(!SoilCareData.applyMulch(server,soil)) {player.displayClientMessage(Component.literal("This plot is already mulched."),true);return InteractionResult.PASS;}
            if(!player.getAbilities().instabuild)held.shrink(1);
            server.levelEvent(2005,soil.above(),0);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext ctx,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.literal("Use on farmland or its crop. Lasts one Minecraft day."));
        lines.add(Component.literal("Halves new weeds and Agri crop nutrition loss."));
        lines.add(Component.literal("Mulched upgraded farmland dries more slowly."));
        lines.add(Component.literal("Check remaining time with the Soil Sampler or Farm Ledger."));
    }
}
