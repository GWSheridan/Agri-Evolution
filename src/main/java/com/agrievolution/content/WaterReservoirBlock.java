package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Twenty-seven coordinated pieces; only the centre owns inventory/water. */
public final class WaterReservoirBlock extends Block implements EntityBlock {
    public static final IntegerProperty X=IntegerProperty.create("part_x",0,2),Y=IntegerProperty.create("part_y",0,2),Z=IntegerProperty.create("part_z",0,2);
    public static final IntegerProperty FILL=IntegerProperty.create("fill",0,4);
    public WaterReservoirBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(X,1).setValue(Y,1).setValue(Z,1).setValue(FILL,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(X,Y,Z,FILL);}

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        int x = state.getValue(X);
        int z = state.getValue(Z);
        // X and Z locate this part within the shell, with the tank at (1, 1, 1).
        return switch (rotation) {
            case CLOCKWISE_90 -> state.setValue(X, 2 - z).setValue(Z, x);
            case CLOCKWISE_180 -> state.setValue(X, 2 - x).setValue(Z, 2 - z);
            case COUNTERCLOCKWISE_90 -> state.setValue(X, z).setValue(Z, 2 - x);
            case NONE -> state;
        };
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(Z, 2 - state.getValue(Z));
            case FRONT_BACK -> state.setValue(X, 2 - state.getValue(X));
            case NONE -> state;
        };
    }

    public static BlockPos core(BlockPos pos,BlockState state){return pos.offset(1-state.getValue(X),1-state.getValue(Y),1-state.getValue(Z));}
    public static boolean port(BlockState state,Direction side){
        int x=state.getValue(X),y=state.getValue(Y),z=state.getValue(Z);
        return switch(side){
            case NORTH -> x==1&&y==1&&z==0;case SOUTH -> x==1&&y==1&&z==2;
            case WEST -> x==0&&y==1&&z==1;case EAST -> x==2&&y==1&&z==1;
            case UP -> x==1&&y==2&&z==1;case DOWN -> x==1&&y==0&&z==1;
        };
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return core(pos,state).equals(pos)?new WaterReservoirBlockEntity(pos,state):null;}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean piston){if(!level.isClientSide)level.scheduleTick(pos,this,20);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        var center=core(pos,state);
        if(center.equals(pos)&&level.getBlockEntity(center) instanceof WaterReservoirBlockEntity tank)tank.generationSecond();
        if(ReservoirStructure.loaded(level,center)&&!ReservoirStructure.complete(level,center)) {
            ReservoirStructure.dismantle(level,center,null);
            return;
        }
        level.scheduleTick(pos,this,20);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean piston){
        boolean dismantled = !state.is(replacement.getBlock());
        if (dismantled) ReservoirStructure.dismantle(level, core(pos, state), pos);
        super.onRemove(state,level,pos,replacement,piston);
        if (dismantled) IrrigationNetwork.updateComparators(level, pos, this);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){
        var tank=tank(level,pos,state);return tank==null||!tank.complete()?0:tank.comparatorSignal();
    }
    private WaterReservoirBlockEntity tank(Level level,BlockPos pos,BlockState state){
        var center=core(pos,state);return level.hasChunkAt(center)&&level.getBlockEntity(center) instanceof WaterReservoirBlockEntity tank?tank:null;
    }

    static String inspectionStatus(Level level, BlockPos pos, BlockState state) {
        var center = core(pos, state);
        var status = ReservoirStructure.status(level, center);
        if (status == ReservoirStructure.Status.COMPLETE && level.getBlockEntity(center) instanceof WaterReservoirBlockEntity tank) {
            String tier = tank.renewable() ? "Reservoir Mk II (1000 mB / 10 sec)" : "Reservoir Mk I (finite)";
            String upgrade = tank.renewable() ? "" : " Use a Conduit to upgrade.";
            return tier + " holds " + tank.water() + " / " + WaterReservoirBlockEntity.CAPACITY + " mB. Fill or drain with buckets." + upgrade;
        }
        return status.message;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if (!level.isClientSide) player.displayClientMessage(Component.literal(inspectionStatus(level, pos, state)), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        boolean upgrading = stack.is(Items.CONDUIT);
        boolean filling = stack.is(Items.WATER_BUCKET);
        if (!upgrading && !filling && !stack.is(Items.BUCKET)) {
            return stack.getItem() instanceof BlockItem ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            var tank = tank(level, pos, state);
            boolean transferred = tank != null && (upgrading ? tank.upgrade() : filling ? tank.addBucket() : tank.takeBucket());
            if (transferred) {
                if (upgrading) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    player.displayClientMessage(Component.literal("Reservoir Mk II generates 1000 mB every 10 seconds."), true);
                } else if (!player.getAbilities().instabuild) {
                    var result = new ItemStack(filling ? Items.BUCKET : Items.WATER_BUCKET);
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
                }
            } else {
                var status = ReservoirStructure.status(level, core(pos, state));
                String message;
                if (status != ReservoirStructure.Status.COMPLETE) message = status.message;
                else if (upgrading) message = "The reservoir is already Mk II.";
                else message = filling ? "Reservoir needs space for a full 1000 mB bucket."
                            : "Reservoir has less than 1000 mB to collect.";
                player.displayClientMessage(Component.literal(message), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
