package com.agrievolution.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
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

    static CompoundTag snapshot(Level level, AECropBlockEntity plant) {
        var tag = new CompoundTag();
        plant.saveAdditional(tag, level.registryAccess());
        return tag;
    }

    static boolean currentPlant(Level level, BlockPos pos, BlockState state, AECropBlockEntity plant, CompoundTag before) {
        return plant.getLevel() == level && plant.getBlockPos().equals(pos) && WorkstationAccess.isCurrent(plant)
                && level.getBlockState(pos) == state && (before == null || before.equals(snapshot(level, plant)));
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.literal("Use on an Agri crop to install or upgrade its trellis."));
        lines.add(Component.literal("Two parents: +"+fieldBonus(tier)+" mutation points (weaker tier applies)."));
        lines.add(Component.literal("Tier 3 lab: +"+labBonus(tier)+" hybrid mutation points."));
        lines.add(Component.literal("Recover with a Spanner while crouching, or by breaking the crop."));
        lines.add(Component.literal(tier==3?"Permanent. Prevents all weeds.":"Installed life: "+remainingTime(remaining(stack))+". "+(tier==2?"90% fewer weed attempts.":"Normal weed chance.")));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();BlockPos pos=context.getClickedPos().immutable();
        var player=context.getPlayer();
        var held=context.getItemInHand();var beforeHeld=held.copy();
        if(held.getItem()!=this || !WorkstationAccess.mayCompleteItemUse(level,player,context.getHand(),held,beforeHeld)
                || !level.hasChunkAt(pos))return InteractionResult.FAIL;
        var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof AgriCropBlock)||tier(state)>=tier)return InteractionResult.PASS;
        var entity=level.getBlockEntity(pos);
        if(!(entity instanceof AECropBlockEntity plant) || !currentPlant(level,pos,state,plant,null))return InteractionResult.FAIL;
        var beforePlant=snapshot(level,plant);
        // Permission hooks may change access, wear or crop records without replacing the block.
        if(!WorkstationAccess.mayUse(level,pos,player,context.getClickedFace(),held)
                || !WorkstationAccess.mayCompleteItemUse(level,player,context.getHand(),held,beforeHeld)
                || !currentPlant(level,pos,state,plant,beforePlant))return InteractionResult.FAIL;
        int previous=tier(state);
        int remaining=remaining(held);
        if(remaining==0)return InteractionResult.FAIL;
        var returned=plant.recoverSupport();
        if(!level.isClientSide) {
            var installed=state.setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,tier);
            if(tier==3)installed=installed.setValue(AgriCropBlock.WEEDY,false);
            if(!level.setBlock(pos,installed,Block.UPDATE_ALL))return InteractionResult.FAIL;
            if(level.getBlockEntity(pos)==plant && tier(level.getBlockState(pos))==tier)plant.setSupportTicks(remaining);
            if(!player.getAbilities().instabuild) {
                held.shrink(1);
                if(previous>0) {
                    if(!player.getInventory().add(returned))Block.popResource(level,pos,returned);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
