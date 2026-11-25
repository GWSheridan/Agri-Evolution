package com.agrievolution.content;

import com.agrievolution.platform.Game;
import com.agrievolution.platform.GameHooks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;

/** Preserves third-party loot while removing only vanilla wheat seed items from
 * vanilla short/tall grass. The existing additive loot modifiers award Wild Seeds. */
public final class StarterProgressionEvents {
    private static final String RECEIVED = "AgriEvolutionStarterJournalV1";
    private StarterProgressionEvents() { }

    public static void onGrassDrops(net.minecraft.world.level.block.state.BlockState state,
                                    java.util.List<net.minecraft.world.entity.item.ItemEntity> drops) {
        if (!state.is(Blocks.SHORT_GRASS) && !state.is(Blocks.TALL_GRASS)) return;
        drops.removeIf(drop -> drop.getItem().is(Items.WHEAT_SEEDS));
    }

    public static void onPlayerJoin(net.minecraft.server.level.ServerPlayer player) {
        giveBooks(player);
        FarmingMilestones.sync(player);
    }
    static void giveBooks(Player player) {
        if(player.level().isClientSide)return;
        var root=Game.hooks().persistentData(player);var flags=root.getCompound(GameHooks.PLAYER_PERSISTED);
        if(root.getBoolean(RECEIVED))flags.putBoolean(RECEIVED,true);
        giveOnce(player,flags,RECEIVED,AEItems.BREEDING_JOURNAL.get());
        giveOnce(player,flags,"AgriEvolutionBuildingJournalV1",AEItems.JOURNAL.get());
        root.put(GameHooks.PLAYER_PERSISTED,flags);
    }
    private static void giveOnce(Player player,net.minecraft.nbt.CompoundTag flags,String key,net.minecraft.world.item.Item item){
        if(flags.getBoolean(key))return;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
                flags.putBoolean(key, true);
                return;
            }
        }
        var stack = new ItemStack(item);
        int slot = inventory.getFreeSlot();
        if (slot >= 0) {
            inventory.setItem(slot, stack);
            inventory.setChanged();
            flags.putBoolean(key, true);
        } else if (player.drop(stack, false) != null) {
            flags.putBoolean(key, true);
        }
    }
    public static void onClone(Player originalPlayer, Player newPlayer){
        var old=Game.hooks().persistentData(originalPlayer);var next=Game.hooks().persistentData(newPlayer);
        var flags=next.getCompound(GameHooks.PLAYER_PERSISTED);
        var original=old.getCompound(GameHooks.PLAYER_PERSISTED);
        GardenWork.copyOrders(original, flags);
        for(String key:new String[]{RECEIVED,"AgriEvolutionBuildingJournalV1"})
            if(original.getBoolean(key)||old.getBoolean(key))flags.putBoolean(key,true);
        next.put(GameHooks.PLAYER_PERSISTED,flags);
    }
}
