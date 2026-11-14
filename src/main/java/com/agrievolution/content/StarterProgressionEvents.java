package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Preserves third-party loot while removing only vanilla wheat seed items from
 * vanilla short/tall grass. The existing additive loot modifiers award Wild Seeds. */
public final class StarterProgressionEvents {
    private static final String RECEIVED = "AgriEvolutionStarterJournalV1";
    private StarterProgressionEvents() { }

    public static void onGrassDrops(BlockDropsEvent event) {
        if (!event.getState().is(Blocks.SHORT_GRASS) && !event.getState().is(Blocks.TALL_GRASS)) return;
        event.getDrops().removeIf(drop -> drop.getItem().is(Items.WHEAT_SEEDS));
    }

    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        giveBooks(event.getEntity());
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) FarmingMilestones.sync(player);
    }
    static void giveBooks(Player player) {
        if(player.level().isClientSide)return;
        var root=player.getPersistentData();var flags=root.getCompound(Player.PERSISTED_NBT_TAG);
        if(root.getBoolean(RECEIVED))flags.putBoolean(RECEIVED,true);
        giveOnce(player,flags,RECEIVED,AEItems.BREEDING_JOURNAL.get());
        giveOnce(player,flags,"AgriEvolutionBuildingJournalV1",AEItems.JOURNAL.get());
        root.put(Player.PERSISTED_NBT_TAG,flags);
    }
    private static void giveOnce(Player player,net.minecraft.nbt.CompoundTag flags,String key,net.minecraft.world.item.Item item){
        if(flags.getBoolean(key))return;
        flags.putBoolean(key,true);
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(item))return;
        var stack=new ItemStack(item);if(!player.getInventory().add(stack))player.drop(stack,false);
    }
    public static void onClone(PlayerEvent.Clone event){
        var old=event.getOriginal().getPersistentData();var next=event.getEntity().getPersistentData();
        var flags=next.getCompound(Player.PERSISTED_NBT_TAG);
        var original=old.getCompound(Player.PERSISTED_NBT_TAG);
        GardenWork.copyOrders(original, flags);
        for(String key:new String[]{RECEIVED,"AgriEvolutionBuildingJournalV1"})
            if(original.getBoolean(key)||old.getBoolean(key))flags.putBoolean(key,true);
        next.put(Player.PERSISTED_NBT_TAG,flags);
    }
}
