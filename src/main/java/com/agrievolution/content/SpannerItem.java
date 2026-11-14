package com.agrievolution.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import org.joml.Vector3f;

/** Nonconsumable assembly tool; no soil research behavior. */
public final class SpannerItem extends Item {
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
        if (block instanceof VerticalGreenhouseBlock) pos = pos.below(state.getValue(VerticalGreenhouseBlock.SECTION));
        showRoute(serverPlayer, IrrigationNetwork.trace(level, pos));
    }

    private static void showRoute(ServerPlayer player, IrrigationNetwork.Route route) {
        var end = route.blocks().getLast();
        String status = switch (route.flow()) {
            case READY -> "Water source ready";
            case EMPTY -> "Water source has less than a bucket";
            case CLOSED_VALVE -> "Valve is closed or powered";
            case DISABLED_PORT -> "Disabled " + route.disabledSide().getName() + " pipe port";
            case DISCONNECTED -> "No connected water source";
            case TOO_LARGE -> "Pipe network exceeds " + IrrigationNetwork.MAX_NODES + " nodes";
        };
        player.displayClientMessage(Component.literal(status + " [" + end.toShortString() + "]"), true);
        var routeColor = new DustParticleOptions(new Vector3f(0.20f, 0.77f, 1.0f), 0.95f);
        var endColor = new DustParticleOptions(route.flow() == IrrigationNetwork.Flow.READY
                ? new Vector3f(0.3f, 0.95f, 0.25f) : new Vector3f(1.0f, 0.44f, 0.15f), 1.25f);
        var level = player.serverLevel();
        int stride = Math.max(1, (route.blocks().size() + 47) / 48);
        for (int i = 0; i < route.blocks().size() - 1; i += stride) {
            var point = route.blocks().get(i);
            level.sendParticles(player, routeColor, true, point.getX() + 0.5, point.getY() + 0.65, point.getZ() + 0.5, 4, 0.18, 0.15, 0.18, 0);
        }
        level.sendParticles(player, endColor, true, end.getX() + 0.5, end.getY() + 0.8, end.getZ() + 0.5, 10, 0.23, 0.22, 0.23, 0);
    }

    public static void onRightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        var player=event.getEntity();var level=event.getLevel();var pos=event.getPos();var state=level.getBlockState(pos);
        if(event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!(player.getMainHandItem().getItem() instanceof SpannerItem)||!player.mayBuild())return;
        if(!(state.getBlock() instanceof IrrigationPipeBlock)&&!(state.getBlock() instanceof IrrigationSprinklerBlock))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if(level.isClientSide)return;
        if(state.getBlock() instanceof IrrigationPipeBlock){
            var side=event.getHitVec().getDirection();IrrigationPipeBlock.toggle(level,pos,side);
            player.displayClientMessage(Component.literal(side.getName()+" pipe port: "+(IrrigationPipeBlock.enabled(level.getBlockState(pos),side)?"enabled":"disabled")),true);
        }else if(level.getBlockEntity(pos) instanceof IrrigationSprinklerBlockEntity head){
            head.configure(player.isShiftKeyDown());player.displayClientMessage(Component.literal(head.controls()),true);
        }
    }
    public SpannerItem(Properties properties){super(properties);}

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Crouch-use on an Agri crop to recover its sticks."));
        lines.add(Component.literal("Crouch and left-click an irrigation fitting to trace water."));
    }

    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        var state = level.getBlockState(pos);
        if (player.isShiftKeyDown() && CropSupportItem.tier(state) > 0) {
            if (!player.mayBuild() || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
            if (!level.isClientSide) {
                if (!(level.getBlockEntity(pos) instanceof AECropBlockEntity plant)) return InteractionResult.FAIL;
                var recovered = plant.recoverSupport();
                var unsupported = state.setValue(AgriCropBlock.SUPPORTED, false).setValue(AgriCropBlock.SUPPORT_TIER, 0);
                if (!level.setBlock(pos, unsupported, Block.UPDATE_ALL)) return InteractionResult.FAIL;
                plant.setSupportTicks(-1);
                if (!recovered.isEmpty() && !player.getAbilities().instabuild && !player.getInventory().add(recovered)) {
                    Block.popResource(level, pos, recovered);
                }
                player.displayClientMessage(Component.literal("Crop sticks removed"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!state.is(Blocks.COPPER_BLOCK))return InteractionResult.PASS;
        if (!level.isClientSide) {
            boolean formed = ReservoirStructure.tryForm(level, pos);
            if (formed) FarmingMilestones.RESERVOIR.award(player);
            player.displayClientMessage(Component.literal(formed
                    ? "Copper Reservoir formed: 1000 / 32000 mB. Connect pipes to its face-centre ports."
                    : "Build a 3x3x3 shell of 26 fresh copper blocks around one water source, then use the Spanner."), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
