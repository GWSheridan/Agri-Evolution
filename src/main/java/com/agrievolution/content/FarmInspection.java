package com.agrievolution.content;

import java.util.*;
import com.agrievolution.AgriEvolution;
import com.agrievolution.farming.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative, bounded read-only observations. No client requests or world edits. */
@EventBusSubscriber(modid=AgriEvolution.MOD_ID)
public final class FarmInspection {
    public static boolean spanner(Player p){return p.getMainHandItem().getItem() instanceof SpannerItem || p.getOffhandItem().getItem() instanceof SpannerItem;}
    public static boolean sampler(Player p){return p.getMainHandItem().getItem() instanceof SoilSampleItem || p.getOffhandItem().getItem() instanceof SoilSampleItem;}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%10!=0||(!spanner(p)&&!sampler(p)))return;
        var hit=p.pick(6,0,false);
        if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK)return;
        var pos=block.getBlockPos();var lines=describe(p.serverLevel(),pos,spanner(p),sampler(p));
        PacketDistributor.sendToPlayer(p,new InspectionPayload(p.level().dimension().location().toString(),pos,String.join("\n",lines)));
    }
    public static List<String> describe(ServerLevel level,BlockPos pos,boolean spanner,boolean sampler){
        var lines=new ArrayList<String>();if(!level.hasChunkAt(pos))return lines;
        var state=level.getBlockState(pos);
        if(spanner && (IrrigationNetwork.port(state,Direction.UP)||state.getBlock() instanceof WaterReservoirBlock||state.getBlock() instanceof IrrigationPipeBlock)){
            lines.add(state.getBlock().getName().getString());
            if(state.getBlock() instanceof WaterReservoirBlock){
                var core=WaterReservoirBlock.core(pos,state);
                if(level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank){
                    lines.add("Water: "+tank.water()+" / 32000 mB");lines.add("Comparator: "+tank.comparatorSignal()+" / 15");
                    lines.add(tank.renewable()?"Mk II: free 1000 mB / 10 seconds":"Mk I: finite water; install a Conduit for Mk II");
                    lines.add(tank.complete()?"Structure complete; ports at face centres":"Structure incomplete");
                }return lines;
            }
            if(state.getBlock() instanceof IrrigationBasinBlock){
                lines.add("Basin: "+(state.getValue(IrrigationBasinBlock.FILLED)?"1000 mB available":"empty"));
                lines.add("Aim at the sprinkler for operating status");return lines;
            }
            var network=IrrigationNetwork.search(level,pos);
            int water = network.supplies().stream().mapToInt(supply -> supply.water(level)).sum();
            lines.add("Reachable sources: "+network.supplies().size()+" | "+water+" mB");
            if(network.overflow())lines.add("Network exceeds 512 nodes");
            if(state.getBlock() instanceof IrrigationPipeBlock){
                String disabled=java.util.Arrays.stream(Direction.values()).filter(d->!IrrigationPipeBlock.enabled(state,d)).map(Direction::getName).collect(java.util.stream.Collectors.joining(", "));
                lines.add("Disabled ports: "+(disabled.isEmpty()?"none":disabled));lines.add("Click a face with Spanner to toggle its port");
            }
            if(state.getBlock() instanceof IrrigationValveBlock)lines.add(IrrigationValveBlock.permitsFlow(level,pos,state)?"Valve open; flow allowed":"Valve closed or redstone-powered");
            if(level.getBlockEntity(pos) instanceof IrrigationSprinklerBlockEntity head){
                lines.add("Head: "+head.water()+" / 4000 mB | Comparator: "+head.comparatorSignal()+" / 15");
                lines.add(head.controls());
                var soils = head.wateredSoils();
                lines.add(head.inspectionStatus(network, soils));
                lines.add("Reachable farmland: "+soils.size()+" / 81");
                lines.add("Preview: green soil; red blocked; amber no soil");
            }else lines.add("Aim at the sprinkler for operating status");
        }else if(sampler && (state.getBlock() instanceof CropBlock || state.is(net.minecraft.tags.BlockTags.CROPS))){
            lines.add(state.getBlock().getName().getString());
            if(state.getBlock() instanceof CropBlock crop)lines.add("Growth: "+crop.getAge(state)+" / "+crop.getMaxAge());
            lines.add("Weeds: "+(CropWeeds.hasWeeds(level,pos)?"present - use a rake":"clear"));
            if(level.getBlockEntity(pos) instanceof AECropBlockEntity plant && state.getBlock() instanceof AgriCropBlock crop){
                var genes=plant.genome();lines.add("Genes G/Y/R: "+genes.growth()+" / "+genes.yield()+" / "+genes.resilience());
                lines.add("Nutrition: "+plant.soilNutrition()+" | Moisture: "+FarmEnvironment.moisture(level,pos.below()));
                String companion = CompanionPlanting.describe(level, pos, crop.species());
                if (!companion.isEmpty()) lines.add("Companion: " + companion);
                int supportTier = CropSupportItem.tier(state);
                lines.add("Support: "+new String[]{"none","wood","iron","obsidian"}[supportTier]+" | Tended: "+(plant.hasBeenTended()?"yes":"no"));
                if (supportTier == 3) {
                    lines.add("Stick life: permanent");
                } else if (supportTier > 0) {
                    int remaining = plant.supportTicks();
                    lines.add("Stick life: " + CropSupportItem.remainingTime(remaining)
                            + (remaining <= CropSupportItem.WEAR_WARNING_TICKS ? " (replace soon)" : ""));
                }
                lines.add("Catalyst: x"+ResourceCropRules.growthMultiplier(crop.species(),level,pos)+" | Soil: x"+(1<<SoilResearch.soilTier(level.getBlockState(pos.below()))));
                lines.add("Trait: "+plant.rareTrait().name()+" | Basin: "+(FarmEnvironment.irrigated(level,pos.below())?"active":"none"));
            }else lines.add("Genes and nutrition: not supplied by this crop");
            lines.add("Sprinkler: "+(SprinklerGrowth.covers(level,pos)?"x5 active":"inactive"));
        }
        return List.copyOf(lines);
    }
    private FarmInspection(){}
}
