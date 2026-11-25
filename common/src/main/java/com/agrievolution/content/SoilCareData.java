package com.agrievolution.content;

import com.agrievolution.farming.CropFamily;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/** Soil history belongs to the plot, not the seed or planted block entity. */
public final class SoilCareData extends SavedData {
    public static final int ROTATION_BONUS=10, MULCH_DURATION=24000, MAX_PLOTS=65536;
    private static final Factory<SoilCareData> FACTORY=new Factory<>(SoilCareData::new,SoilCareData::load,null);
    private final LinkedHashMap<Long,Plot> plots=new LinkedHashMap<>();
    private final LinkedHashMap<Long,PendingHarvest> pending=new LinkedHashMap<>();
    private long sequence;
    private static final class Plot {
        CropFamily family=CropFamily.UNKNOWN;
        long mulchedUntil, revision;
    }
    private record PendingHarvest(BlockPos pos, BlockState state, long revision,
                                  java.util.function.BooleanSupplier validDrops) { }
    public static SoilCareData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY,"agri_soil_care");
    }
    private Plot plot(ServerLevel level,BlockPos soil,boolean create) {
        if(!level.hasChunkAt(soil)) return null;
        long key=soil.asLong();
        if(!(level.getBlockState(soil).getBlock() instanceof FarmBlock)) {
            if(plots.remove(key)!=null) setDirty();
            return null;
        }
        var plot=plots.get(key);
        if(plot==null && create) {
            if(plots.size()>=MAX_PLOTS) plots.remove(plots.keySet().iterator().next());
            plot=new Plot(); plots.put(key,plot);
        }
        if(plot!=null && plot.mulchedUntil>level.getGameTime()
                && plot.mulchedUntil-level.getGameTime()>MULCH_DURATION) {
            plot.mulchedUntil=level.getGameTime()+MULCH_DURATION;setDirty();
        }
        return plot;
    }
    public static CropFamily previous(Level level,BlockPos soil) {
        if(!(level instanceof ServerLevel server)) return CropFamily.UNKNOWN;
        var plot=get(server).plot(server,soil,false);
        return plot==null?CropFamily.UNKNOWN:plot.family;
    }
    public static int rotationBonus(Level level,BlockPos cropPos) {
        if(!(level instanceof ServerLevel server) || !server.hasChunkAt(cropPos)) return 0;
        var state=server.getBlockState(cropPos);
        if(!(state.getBlock() instanceof CropBlock) || CropWeeds.ageProperty(state)==null)return 0;
        var current=CropFamily.of(state);var previous=previous(server,cropPos.below());
        return current!=CropFamily.UNKNOWN && previous!=CropFamily.UNKNOWN && current!=previous?ROTATION_BONUS:0;
    }
    public static int mulchRemaining(Level level,BlockPos soil) {
        if(!(level instanceof ServerLevel server)) return 0;
        return get(server).remaining(server,soil);
    }
    int remaining(ServerLevel server,BlockPos soil) {
        var plot=plot(server,soil,false);
        return plot==null?0:(int)Math.clamp(plot.mulchedUntil-server.getGameTime(),0,MULCH_DURATION);
    }
    public static boolean mulched(Level level,BlockPos soil) { return mulchRemaining(level,soil)>0; }
    public static boolean applyMulch(ServerLevel level,BlockPos soil) {
        if(mulched(level,soil)) return false;
        var data=get(level);var plot=data.plot(level,soil,true);if(plot==null)return false;
        plot.mulchedUntil=level.getGameTime()+MULCH_DURATION;data.setDirty();return true;
    }
    public static boolean recordHarvest(ServerLevel level,BlockPos soil,CropFamily family) {
        if(family==CropFamily.UNKNOWN)return false;
        var data=get(level);var plot=data.plot(level,soil,true);if(plot==null)return false;
        plot.family=family;plot.revision=++data.sequence;data.setDirty();return true;
    }
    public static void agriHarvest(Level level,BlockPos cropPos,com.agrievolution.genetics.CropSpecies species) {
        if(!(level instanceof ServerLevel server) || !server.hasChunkAt(cropPos))return;
        var current=server.getBlockState(cropPos);
        // A failed removal must not manufacture a completed harvest.
        if(current.getBlock() instanceof AgriCropBlock crop && crop.species()==species && crop.isMaxAge(current))return;
        recordHarvest(server,cropPos.below(),CropFamily.of(species));
    }
    private long revision(ServerLevel level,BlockPos soil) {
        var p=plot(level,soil,false);return p==null?0:p.revision;
    }
    public static void nativeDrops(ServerLevel level, BlockPos pos, BlockState state,
                                   net.minecraft.world.entity.Entity breaker,
                                   java.util.function.BooleanSupplier validDrops) {
        if (state.getBlock() instanceof AgriCropBlock || !(state.getBlock() instanceof CropBlock crop)
                || !crop.isMaxAge(state) || CropFamily.of(state) == CropFamily.UNKNOWN || !validDrops.getAsBoolean()
                || breaker instanceof net.minecraft.world.entity.player.Player p && p.getAbilities().instabuild) return;
        var data = get(level);
        long key = pos.asLong();
        if (data.pending.size() < 4096 || data.pending.containsKey(key)) {
            data.pending.put(key, new PendingHarvest(pos.immutable(), state, data.revision(level, pos.below()), validDrops));
        }
    }
    public void finishHarvests(ServerLevel level) {
        var completed=new ArrayList<>(pending.values());pending.clear();
        for(var candidate:completed) {
            var pos = candidate.pos();
            if(!candidate.validDrops().getAsBoolean() || !level.hasChunkAt(pos)
                    || revision(level,pos.below())!=candidate.revision() || level.getBlockState(pos)==candidate.state())continue;
            recordHarvest(level,pos.below(),CropFamily.of(candidate.state()));
        }
    }
    public static void nativeGrowth(ServerLevel level, BlockPos pos, BlockState oldState, BlockState grownState) {
        if (!level.hasChunkAt(pos)) return;
        var state = level.getBlockState(pos);
        if(state.getBlock() instanceof AgriCropBlock || !(state.getBlock() instanceof CropBlock crop)
                || state != grownState || oldState.getBlock()!=state.getBlock()
                || crop.getAge(state)<=crop.getAge(oldState) || crop.isMaxAge(state)
                || rotationBonus(level,pos)==0 || level.random.nextInt(5)!=0)return;
        var age=CropWeeds.ageProperty(state);int next=crop.getAge(state)+1;
        if(age==null || state.getValue(age)!=crop.getAge(state) || !age.getPossibleValues().contains(next))return;
        // Preserve modded crop properties such as facing and waterlogging.
        level.setBlock(pos,state.setValue(age,next),2);
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        tag.putInt("Version",1);var records=new ListTag();
        plots.forEach((pos,p)->{var t=new CompoundTag();t.putLong("Soil",pos);t.putString("Family",p.family.id);t.putLong("MulchedUntil",p.mulchedUntil);records.add(t);});
        tag.put("Plots",records);return tag;
    }
    public static SoilCareData load(CompoundTag tag,HolderLookup.Provider registries) {
        if(tag.getInt("Version")!=1)throw new IllegalStateException("Unsupported soil-care save version");
        var data=new SoilCareData();var records=tag.getList("Plots",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(MAX_PLOTS,records.size());i++) {
            var t=records.getCompound(i);var p=new Plot();p.family=CropFamily.fromId(t.getString("Family"));
            p.mulchedUntil=Math.max(0,t.getLong("MulchedUntil"));data.plots.put(t.getLong("Soil"),p);
        }
        return data;
    }
}
