package com.agrievolution.forge;

import com.agrievolution.content.AEBlocks;
import com.agrievolution.content.WaterReservoirBlock;
import com.agrievolution.content.WaterReservoirBlockEntity;
import com.agrievolution.content.WaterTank;
import com.agrievolution.platform.ModInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.DeferredRegister;

final class ForgeReservoirPort extends BlockEntity {
    static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModInfo.ID);
    private static final java.util.function.Supplier<BlockEntityType<ForgeReservoirPort>> TYPE = TYPES.register("reservoir_port",
            () -> BlockEntityType.Builder.of(ForgeReservoirPort::new, AEBlocks.WATER_RESERVOIR.get()).build(null));

    private final LazyOptional<IFluidHandler> fluid = LazyOptional.of(() -> new ForgeFluidHandler(this::tank));

    ForgeReservoirPort(BlockPos pos, BlockState state) {
        super(TYPE.get(), pos, state);
    }

    private WaterTank tank() {
        if (isRemoved() || level == null || !(getBlockState().getBlock() instanceof WaterReservoirBlock)) return null;
        var core = WaterReservoirBlock.core(worldPosition, getBlockState());
        return level.hasChunkAt(core) && level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank ? tank : null;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction face) {
        if (capability == ForgeCapabilities.FLUID_HANDLER && !isRemoved()
                && WaterReservoirBlock.port(getBlockState(), face)) return fluid.cast();
        return super.getCapability(capability, face);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluid.invalidate();
    }

}
