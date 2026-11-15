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
        if(state.getBlock()!=replacement.getBlock())ReservoirStructure.dismantle(level,core(pos,state),pos);
        super.onRemove(state,level,pos,replacement,piston);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){
        var tank=tank(level,pos,state);return tank==null||!tank.complete()?0:tank.comparatorSignal();
    }
    private WaterReservoirBlockEntity tank(Level level,BlockPos pos,BlockState state){
        var center=core(pos,state);return level.hasChunkAt(center)&&level.getBlockEntity(center) instanceof WaterReservoirBlockEntity tank?tank:null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide){var tank=tank(level,pos,state);player.displayClientMessage(Component.literal(tank==null?"Reservoir incomplete":(tank.renewable()?"Reservoir Mk II (1000 mB / 10 sec)":"Reservoir Mk I (finite)")+": "+tank.water()+" / 32000 mB. Buckets fill/drain; Conduit upgrades."),true);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.is(Items.CONDUIT)){
            if(!level.isClientSide){var tank=tank(level,pos,state);boolean upgraded=tank!=null&&tank.upgrade();
                if(upgraded&&!player.getAbilities().instabuild)stack.shrink(1);
                player.displayClientMessage(Component.literal(upgraded?"Reservoir Mk II: generates 1000 mB every 10 seconds, no fuel required.":"Reservoir already upgraded or incomplete."),true);}
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!stack.is(Items.BUCKET)&&!stack.is(Items.WATER_BUCKET))return stack.getItem() instanceof BlockItem
                ?ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION:ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide){
            var tank=tank(level,pos,state);boolean fill=stack.is(Items.WATER_BUCKET);
            if(tank!=null && (fill?tank.addBucket():tank.takeBucket())){
                if(!player.getAbilities().instabuild){
                    ItemStack result=new ItemStack(fill?Items.BUCKET:Items.WATER_BUCKET);stack.shrink(1);
                    if(stack.isEmpty())player.setItemInHand(hand,result);
                    else if(!player.getInventory().add(result))player.drop(result,false);
                }
            }else player.displayClientMessage(Component.literal("Reservoir incomplete, empty, or full."),true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
