package com.agrievolution.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Tiered trellis; the legacy supported=true state remains a wooden support. */
public final class CropSupportItem extends Item {
    static final int WEAR_WARNING_TICKS = 20 * 60;
    private final int tier;
    public static int lifetime(int tier){return tier==1?24000:tier==2?288000:-1;}
    public static String remainingTime(int ticks) {
        long seconds = (Math.max(0, ticks) + 19L) / 20;
        if (seconds >= 3600) return seconds / 3600 + "h " + seconds % 3600 / 60 + "m";
        if (seconds >= 60) return seconds / 60 + "m " + seconds % 60 + "s";
        return seconds + "s";
    }
    public static int remaining(ItemStack stack){
        var tag=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tier(stack)==3?-1:tag.contains("SupportTicks")?Math.clamp(tag.getInt("SupportTicks"),0,lifetime(tier(stack))):lifetime(tier(stack));
    }
    public static int weedChance(int tier){return tier==3?0:tier==2?800:80;}
    public CropSupportItem(Properties properties) { this(properties,1); }
    public CropSupportItem(Properties properties,int tier) { super(properties);this.tier=tier; }
    public static int tier(ItemStack stack) { return stack.getItem() instanceof CropSupportItem support?support.tier:0; }
    public static int tier(BlockState state) {
        return state.hasProperty(AgriCropBlock.SUPPORTED)&&state.getValue(AgriCropBlock.SUPPORTED)
                ?Math.max(1,state.getValue(AgriCropBlock.SUPPORT_TIER)):0;
    }
    public static int fieldBonus(int tier) { return switch(tier){case 1->5;case 2->8;case 3->12;default->0;}; }
    public static int labBonus(int tier) { return tier==0?0:4+4*tier; }
    public static Item itemFor(int tier) { return tier==3?AEItems.OBSIDIAN_CROP_SUPPORT.get():tier==2?AEItems.IRON_CROP_SUPPORT.get():AEItems.CROP_SUPPORT.get(); }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.literal("Use on an Agri crop to install or upgrade its trellis."));
        lines.add(Component.literal("Two parents: +"+fieldBonus(tier)+" mutation points (weaker tier applies)."));
        lines.add(Component.literal("Tier 3 lab: +"+labBonus(tier)+" hybrid mutation points."));
        lines.add(Component.literal("Recover with a Spanner while crouching, or by breaking the crop."));
        lines.add(Component.literal(tier==3?"Permanent. Prevents all weeds.":"Installed life: "+remainingTime(remaining(stack))+". "+(tier==2?"90% fewer weed attempts.":"Normal weed chance.")));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();BlockPos pos=context.getClickedPos();var state=level.getBlockState(pos);
        var player=context.getPlayer();
        if(!(state.getBlock() instanceof AgriCropBlock)||tier(state)>=tier)return InteractionResult.PASS;
        if(player==null||!player.mayBuild()||!level.mayInteract(player,pos)
                ||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        int previous=tier(state);
        int remaining=remaining(context.getItemInHand());
        if(remaining==0)return InteractionResult.FAIL;
        var returned=level.getBlockEntity(pos) instanceof AECropBlockEntity plant?plant.recoverSupport():new ItemStack(itemFor(Math.max(1,previous)));
        if(!level.isClientSide&&level.setBlock(pos,state.setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,tier),3)) {
            if(level.getBlockEntity(pos) instanceof AECropBlockEntity plant)plant.setSupportTicks(remaining);
            if(tier==3)level.setBlock(pos,level.getBlockState(pos).setValue(AgriCropBlock.WEEDY,false),3);
            if(!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
                if(previous>0) {
                    if(!player.getInventory().add(returned))Block.popResource(level,pos,returned);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
