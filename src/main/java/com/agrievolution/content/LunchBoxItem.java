package com.agrievolution.content;

import java.util.List;
import com.agrievolution.AgriEvolution;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Persistent food storage. Consumption is server-owned and bounded per player. */
@EventBusSubscriber(modid=AgriEvolution.MOD_ID)
public final class LunchBoxItem extends Item {
    public static final int SLOTS=9;
    private static final String PAUSED="LunchBoxPaused";
    private static final String PRIORITIES="LunchBoxPriorities", PROTECTED="LunchBoxProtected";
    private static final java.util.Set<Player> FEEDING = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    public static int priority(ItemStack box,int slot){
        int[] values=box.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntArray(PRIORITIES);
        return slot>=0&&slot<values.length?Math.clamp(values[slot],0,2):1;
    }
    public static boolean protectedSlot(ItemStack box,int slot){
        return slot>=0&&slot<SLOTS&&(box.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt(PROTECTED)&(1<<slot))!=0;
    }
    public static void cyclePriority(ItemStack box,int slot){
        if(slot<0||slot>=SLOTS)return;
        int[] values=new int[SLOTS];for(int i=0;i<SLOTS;i++)values[i]=priority(box,i);
        values[slot]=(values[slot]+1)%3;
        CustomData.update(DataComponents.CUSTOM_DATA,box,tag->tag.putIntArray(PRIORITIES,values));
    }
    public static void toggleProtected(ItemStack box,int slot){
        if(slot>=0&&slot<SLOTS)CustomData.update(DataComponents.CUSTOM_DATA,box,tag->tag.putInt(PROTECTED,tag.getInt(PROTECTED)^(1<<slot)));
    }
    /** Highest unprotected priority first; ties retain left-to-right order. */
    public static int selectFood(ItemStack box,List<ItemStack> foods,Player player){
        int selected=-1,best=-1;
        for(int i=0;i<Math.min(SLOTS,foods.size());i++){
            var stack=foods.get(i);var food=stack.getFoodProperties(player);
            if(stack.isEmpty()||stack.getItem() instanceof LunchBoxItem||food==null||food.nutrition()<=0||protectedSlot(box,i))continue;
            if(priority(box,i)>best){selected=i;best=priority(box,i);}
        }
        return selected;
    }
    /** Supply state shown while the menu is open; feeding itself waits until it closes. */
    public static int feedingStatus(ItemStack box,Player player){
        if(!enabled(box))return 1;
        if(!player.isAlive()||player.isCreative()||player.isSpectator())return 2;
        if(!player.getFoodData().needsFood())return 3;
        var foods=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        box.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(foods);
        if(selectFood(box,foods,player)>=0)return 0;
        for(int i=0;i<SLOTS;i++){
            var food=foods.get(i).getFoodProperties(player);
            if(!foods.get(i).isEmpty()&&!(foods.get(i).getItem() instanceof LunchBoxItem)&&food!=null&&food.nutrition()>0)return 4;
        }
        return 5;
    }
    public static String statusLabel(int status){return switch(status){
        case 1->"Paused";case 2->"Inactive player";case 3->"Hunger full";
        case 4->"All food protected";case 5->"No usable food";default->"Ready when closed";
    };}
    public LunchBoxItem(Properties properties){super(properties);}
    public static boolean accepts(ItemStack stack){
        return !stack.isEmpty() && !(stack.getItem() instanceof LunchBoxItem)
                && stack.getFoodProperties(null)!=null;
    }
    public static boolean enabled(ItemStack box){
        return !box.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBoolean(PAUSED);
    }
    public static void toggle(ItemStack box){
        boolean pause=enabled(box);
        CustomData.update(DataComponents.CUSTOM_DATA,box,tag->tag.putBoolean(PAUSED,pause));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack box=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){
            if(player.isShiftKeyDown()){
                toggle(box);player.getInventory().setChanged();
                player.displayClientMessage(Component.literal("Lunch Box: auto-feed "+(enabled(box)?"on":"paused")),true);
            }else{
                int slot=hand==InteractionHand.MAIN_HAND?player.getInventory().selected:40;
                server.openMenu(new SimpleMenuProvider((id,inv,who)->new LunchBoxMenu(id,inv,slot),
                        Component.translatable("container.agri_evolution.lunch_box")),buf->buf.writeVarInt(slot));
            }
        }
        return InteractionResultHolder.sidedSuccess(box,level.isClientSide);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer player && player.tickCount%80==0) feedOne(player);
    }
    /** One serving total, even when several boxes are carried. Never races an open menu. */
    public static boolean feedOne(Player player){
        if(player.level().isClientSide || !player.isAlive() || player.isCreative() || player.isSpectator()
                || !player.getFoodData().needsFood() || player.isUsingItem()
                || player.containerMenu!=player.inventoryMenu || !FEEDING.add(player))return false;
        try { return feedFromInventory(player); }
        finally { FEEDING.remove(player); }
    }
    private static boolean feedFromInventory(Player player){
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            if(slot>=36 && slot!=40)continue;
            ItemStack box=player.getInventory().getItem(slot);
            if(!(box.getItem() instanceof LunchBoxItem)||!enabled(box))continue;
            var foods=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
            box.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(foods);
            int i=selectFood(box,foods,player);
            if(i>=0){
                ItemStack stored=foods.get(i);
                var food=stored.getFoodProperties(player);
                if(stored.isEmpty() || stored.getItem() instanceof LunchBoxItem || food==null || food.nutrition()<=0)continue;
                // Consume a detached single serving so container returns cannot corrupt other portions.
                ItemStack serving=stored.copyWithCount(1);
                foods.set(i,stored.copyWithCount(stored.getCount()-1));
                box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(foods));
                ItemStack remainder=serving.finishUsingItem(player.level(),player);
                if(!remainder.isEmpty())player.getInventory().placeItemBackInInventory(remainder);
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        long occupied=stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyStream().count();
        lines.add(Component.literal(occupied+"/9 food slots | Auto-feed "+(enabled(stack)?"on":"paused")));
        lines.add(Component.literal("Right-click: open | Sneak-use: toggle auto-feed"));
        lines.add(Component.literal("Open to set slot priorities and protect food from auto-feeding."));
        lines.add(Component.literal("Feeds every 4 seconds when hungry. Close menus to eat."));
        lines.add(Component.literal("Normal food effects apply, including harmful effects."));
    }
}
