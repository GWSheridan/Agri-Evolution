package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
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
    private AABBTicket waterTicket;
    /** 0 = original basin, 1 = copper (8 block radius), 2 = amethyst (10 block radius). */
    private int upgradeTier = 0;

    public int upgradeTier() { return upgradeTier; }
    public int wateringRadius() { return 6 + 2 * upgradeTier; }

    /** Server-side research upgrade; the same block entity and inventory ID are preserved. */
    public void upgradeTo(int nextTier) {
        if (!(level instanceof ServerLevel) || nextTier != upgradeTier + 1 || nextTier > 2) return;
        upgradeTier = nextTier;
        stopWatering();
        setChanged();
        BlockState current = getBlockState();
        if (current.getBlock() instanceof IrrigationBasinBlock)
            level.setBlock(worldPosition, current.setValue(IrrigationBasinBlock.UPGRADE, nextTier), 3);
        refreshWatering();
    }

    /** On Mk III, a nearby still-water source automatically refills an empty basin. */
    public static void serverTick(Level world, BlockPos pos, BlockState state, IrrigationBasinBlockEntity basin) {
        if (!(world instanceof ServerLevel server) || basin.isRemoved() || basin.upgradeTier < 2
                || state.getValue(IrrigationBasinBlock.FILLED) || server.getGameTime() % 40 != 0) return;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(side);
            if (!server.hasChunkAt(neighbor)) continue;
            if (server.getFluidState(neighbor).is(FluidTags.WATER) && server.getFluidState(neighbor).isSource()) {
                server.setBlock(pos, state.setValue(IrrigationBasinBlock.FILLED, true), 3);
                basin.refreshWatering();
                return;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IrrigationUpgradeTier", upgradeTier);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        upgradeTier = Math.max(0, Math.min(2, tag.getInt("IrrigationUpgradeTier")));
        stopWatering();
    }

    public IrrigationBasinBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.IRRIGATION_BASIN.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Save migration: existing upgraded basins were stored in block entity NBT only.
        if (level instanceof ServerLevel server && getBlockState().getBlock() instanceof IrrigationBasinBlock
                && getBlockState().getValue(IrrigationBasinBlock.UPGRADE) != upgradeTier)
            server.setBlock(worldPosition, getBlockState().setValue(IrrigationBasinBlock.UPGRADE, upgradeTier), 3);
        refreshWatering();
    }

    /** Called immediately after bucket interaction and when a state is loaded/updated. */
    public void refreshWatering() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;

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
                        server.setBlock(soil, farmland.setValue(FarmBlock.MOISTURE, 7), 2);
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
        super.setBlockState(state);
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
