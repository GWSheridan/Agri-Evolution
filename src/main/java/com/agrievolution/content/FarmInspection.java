package com.agrievolution.content;

import java.util.*;
import com.agrievolution.AgriEvolution;
import com.agrievolution.farming.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
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

    private static boolean irrigationTarget(BlockState state) {
        return IrrigationNetwork.port(state, Direction.UP) || state.getBlock() instanceof WaterReservoirBlock
                || state.getBlock() instanceof IrrigationPipeBlock || state.getBlock() instanceof VerticalGreenhouseBlock;
    }

    private static boolean cropTarget(BlockState state) {
        return state.getBlock() instanceof CropBlock || state.is(BlockTags.CROPS);
    }

    public static boolean canInspect(BlockState state, boolean spanner, boolean sampler) {
        return spanner && irrigationTarget(state) || sampler && cropTarget(state);
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%10!=0||(!spanner(p)&&!sampler(p)))return;
        var hit=p.pick(6,0,false);
        if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK)return;
        var pos=block.getBlockPos();var lines=describe(p.serverLevel(),pos,spanner(p),sampler(p));
        if (lines.isEmpty()) return;
        PacketDistributor.sendToPlayer(p,new InspectionPayload(p.level().dimension().location().toString(),pos,String.join("\n",lines)));
    }
    public static List<String> describe(ServerLevel level,BlockPos pos,boolean spanner,boolean sampler){
        var lines=new ArrayList<String>();if(!level.hasChunkAt(pos))return lines;
        var state=level.getBlockState(pos);
        if (spanner && irrigationTarget(state)) {
            lines.add(state.getBlock().getName().getString());
            if (state.getBlock() instanceof VerticalGreenhouseBlock) {
                var greenhouse = VerticalGreenhouseBlock.controller(level, pos, state);
                if (greenhouse == null || !greenhouse.complete()) {
                    lines.add("Vertical Greenhouse is incomplete");
                } else {
                    lines.add("Water: " + greenhouse.water() + " / " + VerticalGreenhouseBlockEntity.CAPACITY + " mB");
                    var route = IrrigationNetwork.trace(level, greenhouse.getBlockPos());
                    lines.add("Pipe supply: " + SpannerItem.routeStatus(level, route));
                }
                return lines;
            }
            if(state.getBlock() instanceof WaterReservoirBlock){
                var core=WaterReservoirBlock.core(pos,state);
                var status = ReservoirStructure.status(level, core);
                if (status == ReservoirStructure.Status.COMPLETE && level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank) {
                    lines.add("Water: "+tank.water()+" / 32000 mB");lines.add("Comparator: "+tank.comparatorSignal()+" / 15");
                    lines.add(tank.renewable()?"Mk II: free 1000 mB / 10 seconds":"Mk I: finite water; install a Conduit for Mk II");
                }
                lines.add(status.message);
                return lines;
            }
            if(state.getBlock() instanceof IrrigationBasinBlock){
                if (level.getBlockEntity(pos) instanceof IrrigationBasinBlockEntity basin) lines.add(basin.inspectionStatus());
                return lines;
            }
            var entity = level.getBlockEntity(pos);
            if (state.getBlock() instanceof IrrigationValveBlock) {
                if (entity instanceof IrrigationValveBlockEntity valve && !valve.zoneName().isEmpty()) {
                    lines.add("Zone: " + valve.zoneName());
                }
                var zone = IrrigationNetwork.inspectZone(level, pos);
                var amounts = IrrigationNetwork.supplyWater(level, zone.supplies());
                lines.add(SpannerItem.zoneFlow(level, pos, state, zone, amounts));
                if (zone.limit()) {
                    lines.add("Network exceeds " + IrrigationNetwork.MAX_NODES + " nodes");
                } else {
                    lines.add("Zone sources ready: " + amounts.ready() + "/" + zone.supplies().size() + " | " + amounts.total() + " mB total");
                }
                return lines;
            }
            var supplyInspection = IrrigationNetwork.inspectSupply(level, pos);
            var network = supplyInspection.network();
            if (network.overflow()) {
                lines.add("Network exceeds " + IrrigationNetwork.MAX_NODES + " nodes");
            } else {
                var amounts = IrrigationNetwork.supplyWater(level, network.supplies());
                lines.add("Sources ready: " + amounts.ready() + "/" + network.supplies().size() + " | " + amounts.total() + " mB total");
            }
            if(state.getBlock() instanceof IrrigationPipeBlock){
                String disabled=java.util.Arrays.stream(Direction.values()).filter(d->!IrrigationPipeBlock.enabled(state,d)).map(Direction::getName).collect(java.util.stream.Collectors.joining(", "));
                lines.add("Disabled ports: " + (disabled.isEmpty() ? "none" : disabled));
                if (!network.overflow()) {
                    var route = supplyInspection.route(level);
                    lines.add("Pipe supply: " + SpannerItem.routeStatus(level, route) + " [" + route.blocks().getLast().toShortString() + "]");
                }
                lines.add("Use a main-hand Spanner to toggle a port");
            }
            if(entity instanceof IrrigationSprinklerBlockEntity head){
                lines.add("Head: "+head.water()+" / 4000 mB | Comparator: "+head.comparatorSignal()+" / 15");
                lines.add(head.controls());
                var soils = head.wateredSoils();
                lines.add(head.inspectionStatus(supplyInspection, soils));
                lines.add("Reachable farmland: "+soils.size()+" / 81");
                lines.add("Preview: green soil; red blocked; amber no soil");
            }
        } else if (sampler && cropTarget(state)) {
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
