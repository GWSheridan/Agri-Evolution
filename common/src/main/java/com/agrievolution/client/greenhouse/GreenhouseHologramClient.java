package com.agrievolution.client.greenhouse;

import com.agrievolution.content.AEBlocks;
import com.agrievolution.content.BotanicalGreenhouseBlock;
import com.agrievolution.farming.BotanicalGreenhouseLayout;
import com.agrievolution.farming.BotanicalGreenhouseLayout.PreviewCell;
import com.agrievolution.farming.BotanicalGreenhouseLayout.PreviewPart;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-only, textured, transparent multiblock hologram. No ghost blocks are
 * placed into the world, and no extra client/server packets are necessary.
 *
 * Preview always uses BotanicalGreenhouseLayout's coordinate system, so the
 * displayed footprint rotates with the real controller and validates against
 * exactly the same wall/roof checks as the operating machine.
 */
public final class GreenhouseHologramClient {
    private static final int SCAN_RADIUS = 18;
    private static final int HEIGHT_RADIUS = 5;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final Set<BlockPos> HIDDEN = new HashSet<>();

    // Dedicated render storage avoids disturbing Minecraft's main batch.
    private static final ByteBufferBuilder BYTE_BUFFER = new ByteBufferBuilder(262144);
    private static final MultiBufferSource.BufferSource GHOST_BATCH = MultiBufferSource.immediate(BYTE_BUFFER);

    private static ClientLevel lastLevel;
    private static BlockPos nearbyController;
    private static int ticks;

    private GreenhouseHologramClient() {}

    /** Shift + right-click the controller to hide or re-enable the preview. */
    public static void toggle(BlockPos position) {
        BlockPos immutable = position.immutable();
        boolean visible = HIDDEN.remove(immutable);
        if (!visible) HIDDEN.add(immutable);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(
                    visible ? "Greenhouse hologram: visible" : "Greenhouse hologram: hidden"), true);
        }
        ticks = SCAN_INTERVAL_TICKS; // refresh on next client tick
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            lastLevel = null;
            nearbyController = null;
            HIDDEN.clear();
            ticks = 0;
            return;
        }
        if (lastLevel != mc.level) {
            lastLevel = mc.level;
            nearbyController = null;
            HIDDEN.clear();
            ticks = SCAN_INTERVAL_TICKS;
        }
        if (++ticks < SCAN_INTERVAL_TICKS) return;
        ticks = 0;

        BlockPos player = mc.player.blockPosition();
        BlockPos closest = null;
        double minDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                for (int y = -HEIGHT_RADIUS; y <= HEIGHT_RADIUS; y++) {
                    scratch.set(player.getX() + x, player.getY() + y, player.getZ() + z);
                    if (!mc.level.hasChunkAt(scratch)) continue;
                    if (!mc.level.getBlockState(scratch).is(AEBlocks.BOTANICAL_GREENHOUSE.get())) continue;
                    if (HIDDEN.contains(scratch)) continue;
                    double distance = scratch.distSqr(player);
                    if (distance < minDistance) {
                        closest = scratch.immutable();
                        minDistance = distance;
                    }
                }
            }
        }
        nearbyController = closest;
    }

    public static void onRenderLevel(PoseStack pose, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        BlockPos controller = nearbyController;
        if (level == null || mc.player == null || controller == null || mc.screen != null) return;
        if (!level.hasChunkAt(controller)) return;
        BlockState controllerState = level.getBlockState(controller);
        if (!controllerState.is(AEBlocks.BOTANICAL_GREENHOUSE.get()) || HIDDEN.contains(controller)) return;
        Direction facing = controllerState.getValue(BotanicalGreenhouseBlock.FACING);
        if (BotanicalGreenhouseLayout.isComplete(level, controller, facing)) return;

        if (camera.distanceToSqr(Vec3.atCenterOf(controller)) > (SCAN_RADIUS + 4) * (SCAN_RADIUS + 4)) return;

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        try {
            int farmlandCount = 0;
            for (int across = -2; across <= 2; across++) for (int depth = 1; depth <= 5; depth++) {
                BlockPos soil = BotanicalGreenhouseLayout.at(controller, facing, across, depth, -1);
                if (level.hasChunkAt(soil) && BotanicalGreenhouseLayout.previewMatches(
                        level.getBlockState(soil), PreviewPart.OPTIONAL_FARMLAND)) farmlandCount++;
            }
            // If no entrance exists, mark the recommended front-wall column as a door.
            boolean needsDoor = !BotanicalGreenhouseLayout.hasValidDoor(level, controller, facing);
            BlockPos recommendedDoorBottom = BotanicalGreenhouseLayout.at(controller, facing, 1, 0, 0);
            for (PreviewCell cell : BotanicalGreenhouseLayout.previewCells(controller, facing)) {
                BlockPos pos = cell.pos();
                if (!level.hasChunkAt(pos)) continue;
                PreviewPart part = cell.part();
                if (part == PreviewPart.OPTIONAL_FARMLAND && farmlandCount >= 9) continue;
                BlockState actual = level.getBlockState(pos);
                boolean entrySuggestion = needsDoor && part == PreviewPart.ENTRANCE_OR_GLASS
                        && (pos.equals(recommendedDoorBottom) || pos.equals(recommendedDoorBottom.above()));
                if (!entrySuggestion && BotanicalGreenhouseLayout.previewMatches(actual, part)) continue;
                if (entrySuggestion && BotanicalGreenhouseLayout.isDoor(actual)) continue;
                boolean empty = actual.isAir() || actual.canBeReplaced();
                boolean soil = part == PreviewPart.OPTIONAL_FARMLAND;
                // Gold doorway = install any real two-high door here. Existing glass is removable.
                // Blue = missing glass; amber = farmland; red = incorrect solid obstruction.
                int r = entrySuggestion ? 255 : !empty ? 255 : soil ? 244 : 95;
                int g = entrySuggestion ? 204 : !empty ? 75 : soil ? 188 : 230;
                int b = entrySuggestion ? 85 : !empty ? 75 : soil ? 90 : 255;
                int a = soil ? 85 : entrySuggestion ? 165 : empty ? 112 : 150;
                BlockState example = entrySuggestion
                        ? Blocks.OAK_DOOR.defaultBlockState()
                             .setValue(DoorBlock.FACING, facing)
                             .setValue(DoorBlock.HALF, pos.equals(recommendedDoorBottom)
                                     ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER)
                        : soil ? Blocks.FARMLAND.defaultBlockState() : Blocks.GLASS.defaultBlockState();

                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                if (!empty && !entrySuggestion) {
                    // Expand red ghosts a hair so they remain visible over a wrong opaque block.
                    pose.translate(-0.018, -0.018, -0.018);
                    pose.scale(1.036f, 1.036f, 1.036f);
                }
                VertexConsumer ghost = new TintedGhost(GHOST_BATCH.getBuffer(RenderType.translucent()), r, g, b, a);
                MultiBufferSource allTranslucent = ignored -> ghost;
                // Full texture and proper block model UVs, but with transparent colors.
                mc.getBlockRenderer().renderSingleBlock(example, pose, allTranslucent,
                        0x00F000F0, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            GHOST_BATCH.endBatch();
        } finally {
            pose.popPose();
        }
    }

    private static final class TintedGhost implements VertexConsumer {
        private final VertexConsumer parent;
        private final int red, green, blue, opacity;
        private TintedGhost(VertexConsumer parent, int r, int g, int b, int a) {
            this.parent = parent;
            this.red = r; this.green = g; this.blue = b; this.opacity = a;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) { parent.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setUv(float u, float v) { parent.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { parent.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { parent.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { parent.setNormal(x, y, z); return this; }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            parent.setColor(r * red / 255, g * green / 255, b * blue / 255, a * opacity / 255);
            return this;
        }

        @Override public VertexConsumer setColor(int argb) {
            return setColor((argb >> 16) & 255, (argb >> 8) & 255,
                    argb & 255, (argb >>> 24) & 255);
        }
    }
}
