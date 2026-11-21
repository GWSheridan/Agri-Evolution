package com.agrievolution.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import org.joml.Vector3f;

/** Nonconsumable assembly tool; no soil research behavior. */
public final class SpannerItem extends Item {
    private static final String CONTROLLER_POS = "AgriControllerPos";
    private static final String CONTROLLER_DIMENSION = "AgriControllerDimension";

    public static void onLeftClick(LeftClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();
        if (!player.isShiftKeyDown() || !(player.getMainHandItem().getItem() instanceof SpannerItem)
                || !player.canInteractWithBlock(pos, 1.0) || !level.hasChunkAt(pos)) return;
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        if (!(block instanceof VerticalGreenhouseBlock || block instanceof IrrigationSprinklerBlock
                || block instanceof IrrigationPipeBlock || block instanceof IrrigationValveBlock)) return;
        event.setCanceled(true);
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        if (event.getAction() != LeftClickBlock.Action.START || !(player instanceof ServerPlayer serverPlayer)) return;
        showRoute(serverPlayer, IrrigationNetwork.trace(level, pos));
    }

    private static void showRoute(ServerPlayer player, IrrigationNetwork.Route route) {
        var end = route.blocks().getLast();
        var level = player.serverLevel();
        player.displayClientMessage(Component.literal(routeStatus(level, route) + " [" + end.toShortString() + "]"), true);
        var routeColor = new DustParticleOptions(new Vector3f(0.20f, 0.77f, 1.0f), 0.95f);
        var endColor = new DustParticleOptions(route.flow() == IrrigationNetwork.Flow.READY
                ? new Vector3f(0.3f, 0.95f, 0.25f) : new Vector3f(1.0f, 0.44f, 0.15f), 1.25f);
        int stride = Math.max(1, (route.blocks().size() + 47) / 48);
        for (int i = 0; i < route.blocks().size() - 1; i += stride) {
            var point = route.blocks().get(i);
            level.sendParticles(player, routeColor, true, point.getX() + 0.5, point.getY() + 0.65, point.getZ() + 0.5, 4, 0.18, 0.15, 0.18, 0);
        }
        Direction stopFace = route.disabledSide() != null ? route.disabledSide() : route.boundarySide();
        var marker = stopFace == null ? net.minecraft.world.phys.Vec3.atCenterOf(end).add(0, 0.3, 0)
                : net.minecraft.world.phys.Vec3.atCenterOf(end).add(stopFace.getStepX() * 0.46, stopFace.getStepY() * 0.46, stopFace.getStepZ() * 0.46);
        level.sendParticles(player, endColor, true, marker.x, marker.y, marker.z, 10, 0.23, 0.22, 0.23, 0);
    }

    static String routeStatus(Level level, IrrigationNetwork.Route route) {
        var end = route.blocks().getLast();
        return switch (route.flow()) {
            case READY -> "Water source ready" + sourceAmount(route);
            case EMPTY -> "Water source has less than a bucket" + sourceAmount(route);
            case CLOSED_VALVE -> IrrigationValveBlock.namedFlowStatus(level, end, "Valve");
            case RESERVE_HELD -> level.getBlockEntity(end) instanceof IrrigationValveBlockEntity valve
                    ? valve.reserveStatus() : "Refill held in reserve";
            case DISABLED_PORT -> "Disabled " + route.disabledSide().getName() + " pipe port";
            case DISCONNECTED -> "No connected water source";
            case TOO_LARGE -> "Pipe network exceeds " + IrrigationNetwork.MAX_NODES + " nodes";
            case UNLOADED_CONNECTION -> IrrigationNetwork.unloadedMessage(route.boundarySide());
            case UNLOADED_RESERVOIR -> ReservoirStructure.Status.UNLOADED.message;
            case INCOMPLETE_RESERVOIR -> ReservoirStructure.Status.INCOMPLETE.message;
            case PAUSED_BY_REDSTONE -> "Sprinkler paused by redstone";
            case WAITING_FOR_REDSTONE -> "Sprinkler waiting for redstone";
            case INCOMPLETE_GREENHOUSE -> "Vertical Greenhouse is incomplete";
        };
    }

    private static String sourceAmount(IrrigationNetwork.Route route) {
        return route.sourceWater() < 0 ? "" : " (" + route.sourceWater() + " mB)";
    }

    public static void onRightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        var player=event.getEntity();var level=event.getLevel();var pos=event.getPos();
        if(event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!(player.getMainHandItem().getItem() instanceof SpannerItem))return;
        if (!level.hasChunkAt(pos)) return;
        var state = level.getBlockState(pos);
        if(!(state.getBlock() instanceof IrrigationPipeBlock)&&!(state.getBlock() instanceof IrrigationSprinklerBlock)
                && !(state.getBlock() instanceof IrrigationValveBlock))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) return;
        boolean valve = state.getBlock() instanceof IrrigationValveBlock;
        var side = event.getHitVec().getDirection();
        var tool = player.getMainHandItem();
        var beforeTool = tool.copy();
        var entity = level.getBlockEntity(pos);
        boolean crouching = player.isShiftKeyDown();
        if ((!valve || player.isShiftKeyDown()) && (!canEdit(player, level, pos, side, tool)
                || !sameTool(player, level, event.getHand(), tool, beforeTool)
                || player.isShiftKeyDown() != crouching
                || !sameTarget(level, pos, state, entity))) return;
        if (valve) {
            if (player.isShiftKeyDown() && linkController(player, level, pos, side)) return;
            inspectValve(player, level, pos, state);
        }else if(state.getBlock() instanceof IrrigationPipeBlock){
            if (IrrigationPipeBlock.toggle(level, pos, side)) {
                player.displayClientMessage(Component.literal(side.getName()+" pipe port: "+(IrrigationPipeBlock.enabled(level.getBlockState(pos),side)?"enabled":"disabled")),true);
            } else {
                player.displayClientMessage(Component.literal("Load the neighboring chunk to change this pipe port."), true);
            }
        }else if(level.getBlockEntity(pos) instanceof IrrigationSprinklerBlockEntity head){
            head.configure(player.isShiftKeyDown());player.displayClientMessage(Component.literal(head.controls()),true);
        }
    }

    private static boolean linkController(Player player, Level level, BlockPos valvePos, Direction side) {
        ItemStack tool = player.getMainHandItem();
        var beforeTool = tool.copy();
        var selected = tool.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (!selected.contains(CONTROLLER_POS)) return false;
        BlockPos controllerPos = BlockPos.of(selected.getLong(CONTROLLER_POS));
        var valveState = level.getBlockState(valvePos);
        var valveEntity = level.getBlockEntity(valvePos);
        String dimension = level.dimension().location().toString();
        String result = "The selected Irrigation Controller is unavailable.";
        if (dimension.equals(selected.getString(CONTROLLER_DIMENSION)) && level.hasChunkAt(controllerPos)
                && level.getBlockEntity(controllerPos) instanceof IrrigationControllerBlockEntity controller
                && valveEntity instanceof IrrigationValveBlockEntity valve) {
            var controllerState = level.getBlockState(controllerPos);
            if (!canEdit(player, level, controllerPos, side, tool)) {
                result = "You need permission to edit the Irrigation Controller.";
            } else if (sameTool(player, level, net.minecraft.world.InteractionHand.MAIN_HAND, tool, beforeTool)
                    && player.isShiftKeyDown()
                    && sameTarget(level, controllerPos, controllerState, controller)
                    && sameTarget(level, valvePos, valveState, valve)) {
                result = controller.link(valve);
                net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, tool, tag -> {
                    tag.remove(CONTROLLER_POS);
                    tag.remove(CONTROLLER_DIMENSION);
                });
            }
        }
        player.displayClientMessage(Component.literal(result), true);
        return true;
    }
    private static void inspectValve(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        var valve = level.getBlockEntity(pos) instanceof IrrigationValveBlockEntity named ? named : null;
        if (player.isShiftKeyDown()) {
            if (valve == null) {
                valve = new IrrigationValveBlockEntity(pos, state);
                level.setBlockEntity(valve);
            }
            var name = player.getMainHandItem().get(DataComponents.CUSTOM_NAME);
            valve.rename(name == null ? "" : name.getString());
        }
        String name = valve == null ? "Irrigation zone" : valve.zone();
        for (String line : describeZone(level, pos, state, name)) {
            player.displayClientMessage(Component.literal(line), false);
        }
    }

    static List<String> describeZone(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, String name) {
        var status = IrrigationNetwork.inspectZone(level, pos);
        if (status.limit()) {
            String valveStatus = IrrigationValveBlock.flowStatus(level, pos, state);
            return List.of(name + " | " + valveStatus + " | Pipe network exceeds " + IrrigationNetwork.MAX_NODES
                    + " nodes. Supply and sprinkler counts unavailable.");
        }
        var amounts = IrrigationNetwork.supplyWater(level, status.supplies());
        String flow = zoneFlow(level, pos, state, status, amounts);
        String tanks = status.supplies().isEmpty() ? "No reachable water storage"
                : "Supply water: " + amounts.total() + " / " + amounts.capacity() + " mB | Sources ready: "
                + amounts.ready() + "/" + status.supplies().size();
        int active = 0;
        IrrigationSprinklerBlockEntity nearestStopped = null;
        for (BlockPos headPos : status.sprinklers()) {
            if (!(level.getBlockEntity(headPos) instanceof IrrigationSprinklerBlockEntity head)) continue;
            if (head.growthActive()) active++;
            else if (nearestStopped == null) nearestStopped = head;
        }
        var lines = new ArrayList<String>();
        lines.add(name + " | " + flow + " | " + tanks + " | Sprinklers: " + active + "/" + status.sprinklers().size() + " active");
        if (nearestStopped != null) {
            lines.add("Nearest inactive sprinkler [" + nearestStopped.getBlockPos().toShortString() + "]: " + nearestStopped.inspectionStatus());
        }
        return List.copyOf(lines);
    }

    static String zoneFlow(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                           IrrigationNetwork.ZoneStatus status, IrrigationNetwork.SupplyWater amounts) {
        if (!IrrigationValveBlock.permitsFlow(level, pos, state)) return IrrigationValveBlock.flowStatus(level, pos, state);
        if (status.limit()) return "Valve open";
        if (!status.supplies().isEmpty()) {
            return amounts.ready() == 0 ? "Connected sources short of a full bucket" : "Water supply ready";
        }
        if (status.closedValve() != null) {
            BlockPos shutoff = status.closedValve();
            return IrrigationValveBlock.namedFlowStatus(level, shutoff, "Valve")
                    + " along supply path [" + shutoff.toShortString() + "]";
        }
        if (status.disabledSide() != null) {
            return "Supply blocked by disabled " + status.disabledSide().getName() + " pipe port [" + status.disabledPipe().toShortString() + "]";
        }
        if (status.unloaded() != null) {
            var stop = status.unloaded();
            String reason = stop.flow() == IrrigationNetwork.Flow.UNLOADED_RESERVOIR
                    ? ReservoirStructure.Status.UNLOADED.message : IrrigationNetwork.unloadedMessage(stop.side());
            return reason + " [" + stop.fitting().toShortString() + "]";
        }
        if (status.incompleteReservoir()) return ReservoirStructure.Status.INCOMPLETE.message;
        return "No connected water source";
    }
    public SpannerItem(Properties properties){super(properties);}

    private static boolean canEdit(Player player, Level level, BlockPos pos, Direction side, ItemStack tool) {
        return player.level() == level && player.isAlive() && !player.isSpectator() && player.mayBuild()
                && level.hasChunkAt(pos) && level.mayInteract(player, pos) && player.mayUseItemAt(pos, side, tool);
    }

    private static boolean sameTool(Player player, Level level, net.minecraft.world.InteractionHand hand,
                                    ItemStack tool, ItemStack before) {
        return player.level() == level && player.isAlive() && !player.isSpectator() && player.mayBuild()
                && tool.getItem() instanceof SpannerItem && player.getItemInHand(hand) == tool && ItemStack.matches(before, tool);
    }

    private static boolean sameTarget(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                                      net.minecraft.world.level.block.entity.BlockEntity entity) {
        return level.hasChunkAt(pos) && level.getBlockState(pos) == state && level.getBlockEntity(pos) == entity;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Crouch-use on an Agri crop to recover its sticks."));
        lines.add(Component.literal("Crouch and left-click an irrigation fitting to trace water."));
        lines.add(Component.literal("Use on a valve to inspect its zone. Crouch-use a renamed Spanner to name it."));
        lines.add(Component.literal("Crouch-use an Irrigation Controller, then a named valve, to link a zone."));
    }

    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos().immutable();var player=context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        var tool = context.getItemInHand();
        var beforeTool = tool.copy();
        if (!sameTool(player, level, context.getHand(), tool, beforeTool) || !level.hasChunkAt(pos)) return InteractionResult.FAIL;
        var state = level.getBlockState(pos);
        var target = level.getBlockEntity(pos);
        if (player.isShiftKeyDown() && state.is(AEBlocks.IRRIGATION_CONTROLLER.get())) {
            if (!canEdit(player, level, pos, context.getClickedFace(), tool)
                    || !sameTool(player, level, context.getHand(), tool, beforeTool)
                    || !player.isShiftKeyDown()
                    || !sameTarget(level, pos, state, target)) return InteractionResult.FAIL;
            if (!level.isClientSide) {
                net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, tool, tag -> {
                    tag.putLong(CONTROLLER_POS, pos.asLong());
                    tag.putString(CONTROLLER_DIMENSION, level.dimension().location().toString());
                });
                player.displayClientMessage(Component.literal("Controller selected. Crouch-use a named valve to link it."), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown() && CropSupportItem.tier(state) > 0) {
            var held = context.getItemInHand();
            var beforeHeld = held.copy();
            var entity = level.getBlockEntity(pos);
            if (held.getItem() != this || !(entity instanceof AECropBlockEntity plant)
                    || !CropSupportItem.currentPlant(level, pos, state, plant, null)) return InteractionResult.FAIL;
            var beforePlant = CropSupportItem.snapshot(level, plant);
            if (!WorkstationAccess.mayUse(level, pos, player, context.getClickedFace(), held)
                    || !WorkstationAccess.mayCompleteItemUse(level, player, context.getHand(), held, beforeHeld)
                    || !player.isShiftKeyDown()
                    || !CropSupportItem.currentPlant(level, pos, state, plant, beforePlant)) return InteractionResult.FAIL;
            if (!level.isClientSide) {
                var recovered = plant.recoverSupport();
                var unsupported = state.setValue(AgriCropBlock.SUPPORTED, false).setValue(AgriCropBlock.SUPPORT_TIER, 0);
                if (!level.setBlock(pos, unsupported, Block.UPDATE_ALL)) return InteractionResult.FAIL;
                if (level.getBlockEntity(pos) == plant && CropSupportItem.tier(level.getBlockState(pos)) == 0) plant.setSupportTicks(-1);
                if (!recovered.isEmpty() && !player.getAbilities().instabuild && !player.getInventory().add(recovered)) {
                    Block.popResource(level, pos, recovered);
                }
                player.displayClientMessage(Component.literal("Crop sticks removed"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!state.is(Blocks.COPPER_BLOCK))return InteractionResult.PASS;
        if (!level.isClientSide) {
            var result = ReservoirStructure.tryForm(level, pos,
                    part -> sameTool(player, level, context.getHand(), tool, beforeTool)
                            && canEdit(player, level, part, context.getClickedFace(), tool)
                            && sameTool(player, level, context.getHand(), tool, beforeTool)
                            && sameTarget(level, pos, state, target));
            if (result == ReservoirStructure.Formation.FORMED) FarmingMilestones.RESERVOIR.award(player);
            String message = switch (result) {
                case FORMED -> "Copper Reservoir formed with 1000 mB. Connect pipes to its face-centre ports.";
                case PROTECTED -> "You need permission to edit the whole reservoir shell and its centre.";
                case UNLOADED -> "Part of the reservoir shell is in an unloaded chunk. Load the rest of the shell and try again.";
                case INVALID -> "Build a 3x3x3 shell of 26 fresh copper blocks around one water source, then use the Spanner.";
            };
            player.displayClientMessage(Component.literal(message), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
