package com.agrievolution.content;

import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class BuildingJournalItem extends Item {
    public BuildingJournalItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(level.isClientSide)com.agrievolution.client.JournalClientScreens.open(stack);
        else if (player instanceof net.minecraft.server.level.ServerPlayer server) FarmingMilestones.sync(server);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
