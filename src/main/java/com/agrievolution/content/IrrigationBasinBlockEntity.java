package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ticket.AABBTicket;

/**
 * Maintains a NeoForge farmland-water ticket while this basin is full.
 *
 * Unlike periodic moisture updates, this participates in FarmBlock's own
 * isNearWater check, so uncovered farmland cannot dry out between ticks.
 * Tickets are transient: re-register on chunk load, invalidate on unload,
 * removal or emptying. The water-filled block state is saved by Minecraft.
 */
public final class IrrigationBasinBlockEntity extends BlockEntity {
    private static final String UPGRADE_TAG = "IrrigationUpgradeTier";
    private AABBTicket waterTicket;
    /** 0 = original basin, 1 = copper (8 block radius), 2 = amethyst (10 block radius). */
    private int upgradeTier;

    public int upgradeTier() { return upgradeTier; }
    public int wateringRadius() { return wateringRadius(upgradeTier); }
    static int wateringRadius(int tier) { return 6 + 2 * tier; }

    /** Server-side research upgrade; the same block entity and inventory ID are preserved. */
    public boolean upgradeTo(int nextTier) {
        if (!(level instanceof ServerLevel server) || isRemoved() || nextTier != upgradeTier + 1 || nextTier > 2
                || !server.hasChunkAt(worldPosition) || server.getBlockEntity(worldPosition) != this) return false;
        BlockState state = getBlockState().setValue(IrrigationBasinBlock.UPGRADE, nextTier);
        if (!server.setBlock(worldPosition, state, IrrigationNetwork.loadedUpdateFlags(server, worldPosition, Block.UPDATE_CLIENTS))) return false;
        setChanged();
        return true;
    }

    /** On Mk III, a nearby still-water source automatically refills an empty basin. */
    public static void serverTick(Level world, BlockPos pos, BlockState state, IrrigationBasinBlockEntity basin) {
        if (!(world instanceof ServerLevel server) || basin.getLevel() != world || !basin.getBlockPos().equals(pos)
                || !WorkstationAccess.isCurrent(basin) || server.getBlockState(pos) != state || basin.upgradeTier < 2
                || state.getValue(IrrigationBasinBlock.FILLED) || server.getGameTime() % 40 != 0) return;
        if (basin.hasRefillSource()) IrrigationBasinBlock.setFilled(server, pos, state, true);
    }

    String inspectionStatus() {
        String status = "Irrigation Basin Mk " + (upgradeTier + 1) + " | Radius " + wateringRadius() + " blocks | "
                + (getBlockState().getValue(IrrigationBasinBlock.FILLED) ? "1000 mB available" : "Empty");
        if (upgradeTier == 2) status += hasRefillSource() ? " | Auto-refill ready" : " | Needs adjacent source water";
        return status;
    }

    private boolean hasRefillSource() {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = worldPosition.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            var fluid = level.getFluidState(neighbor);
            if (fluid.is(FluidTags.WATER) && fluid.isSource()) return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(UPGRADE_TAG, upgradeTier);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        upgradeTier = tag.contains(UPGRADE_TAG) ? Math.clamp(tag.getInt(UPGRADE_TAG), 0, 2)
                : getBlockState().getValue(IrrigationBasinBlock.UPGRADE);
        stopWatering();
    }

    public IrrigationBasinBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.IRRIGATION_BASIN.get(), pos, state);
        upgradeTier = state.getValue(IrrigationBasinBlock.UPGRADE);
    }

    @Override public void setChanged() {
        if (level == null || !level.hasChunkAt(worldPosition)) return;
        level.blockEntityChanged(worldPosition);
        IrrigationNetwork.updateComparators(level, worldPosition, getBlockState().getBlock());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!WorkstationAccess.isCurrent(this)) return;
        // Save migration: existing upgraded basins were stored in block entity NBT only.
        if (level instanceof ServerLevel server && getBlockState().getBlock() instanceof IrrigationBasinBlock
                && getBlockState().getValue(IrrigationBasinBlock.UPGRADE) != upgradeTier)
            server.setBlock(worldPosition, getBlockState().setValue(IrrigationBasinBlock.UPGRADE, upgradeTier),
                    IrrigationNetwork.loadedUpdateFlags(server, worldPosition, Block.UPDATE_CLIENTS));
        refreshWatering();
    }

    /** Called immediately after bucket interaction and when a state is loaded/updated. */
    public void refreshWatering() {
        if (!(level instanceof ServerLevel server) || !WorkstationAccess.isCurrent(this)) return;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof IrrigationBasinBlock) ||
                !state.getValue(IrrigationBasinBlock.FILLED)) {
            stopWatering();
            return;
        }

        if (waterTicket == null || !waterTicket.isValid()) {
            // Six blocks horizontally (13 x 13 farmland), at basin level +/- 1.
            int r = wateringRadius();
            BlockPos p = getBlockPos();
            waterTicket = FarmlandWaterManager.addAABBTicket(server,
                    new AABB(p.getX() - r, p.getY() - 1, p.getZ() - r,
                             p.getX() + r + 1, p.getY() + 2, p.getZ() + r + 1));
            // The ticket prevents later drying; fill moisture immediately as well.
            hydrateExistingFarmland(server);
        }
    }

    private void hydrateExistingFarmland(ServerLevel server) {
        int radius = wateringRadius();
        BlockPos center = getBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos soil = center.offset(dx, dy, dz);
                    // Do not cause neighboring chunks to load just to water them.
                    if (!server.hasChunkAt(soil)) continue;
                    BlockState farmland = server.getBlockState(soil);
                    if (farmland.getBlock() instanceof FarmBlock &&
                            farmland.getValue(FarmBlock.MOISTURE) != 7) {
                        server.setBlock(soil, farmland.setValue(FarmBlock.MOISTURE, 7),
                                IrrigationNetwork.loadedUpdateFlags(server, soil, Block.UPDATE_CLIENTS));
                    }
                }
            }
        }
    }

    private void stopWatering() {
        if (waterTicket != null) {
            waterTicket.invalidate();
            waterTicket = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation") // NeoForge's block-state setter hook is retained for 1.21.1
    public void setBlockState(BlockState state) {
        boolean tierChanged = state.getBlock() instanceof IrrigationBasinBlock
                && state.getValue(IrrigationBasinBlock.UPGRADE) != getBlockState().getValue(IrrigationBasinBlock.UPGRADE);
        super.setBlockState(state);
        if (tierChanged) {
            upgradeTier = state.getValue(IrrigationBasinBlock.UPGRADE);
            stopWatering();
        }
        refreshWatering();
    }

    @Override
    public void onChunkUnloaded() {
        stopWatering();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        stopWatering();
        super.setRemoved();
    }
}
