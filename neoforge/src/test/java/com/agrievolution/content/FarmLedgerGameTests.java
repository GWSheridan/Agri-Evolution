package com.agrievolution.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class FarmLedgerGameTests {
    private static FarmLedgerData.Field field(FarmLedgerData data, UUID owner) {
        return data.create(owner,"minecraft:overworld",new BlockPos(1,4,1),new BlockPos(2,4,2));
    }
    @GameTest(template="empty") public static void boundsAndOwnershipLimits(GameTestHelper h) {
        var data=new FarmLedgerData();var owner=UUID.randomUUID();
        h.assertTrue(FarmLedgerData.validBounds(BlockPos.ZERO,new BlockPos(31,0,31)),"32x32 allowed");
        h.assertFalse(FarmLedgerData.validBounds(BlockPos.ZERO,new BlockPos(32,0,1)),"33 wide rejected");
        h.assertFalse(FarmLedgerData.validBounds(BlockPos.ZERO,new BlockPos(1,1,1)),"Flat field required");
        h.assertFalse(FarmLedgerData.validBounds(new BlockPos(Integer.MIN_VALUE,0,0),new BlockPos(Integer.MAX_VALUE,0,0)),"Overflow cannot bypass bound");
        for(int i=0;i<16;i++)h.assertTrue(field(data,owner)!=null,"Owned field accepted");
        h.assertTrue(field(data,owner)==null&&field(data,UUID.randomUUID())!=null,"Limits apply per owner");h.succeed();
    }
    @GameTest(template="empty") public static void sharingAndStaleEdits(GameTestHelper h) {
        var data=new FarmLedgerData();var owner=UUID.randomUUID();var viewer=UUID.randomUUID();var editor=UUID.randomUUID();var f=field(data,owner);
        h.assertFalse(f.canView(viewer),"Private by default");data.invite(f,owner,0,viewer,false);data.invite(f,owner,1,editor,true);
        String[] plan={"minecraft:wheat_seeds","","",""};
        h.assertFalse(data.saveLayout(f,viewer,2,0,"North",plan),"View-only cannot save");
        h.assertTrue(data.saveLayout(f,editor,2,0,"North",plan),"Editor can save");
        h.assertFalse(data.saveLayout(f,owner,2,0,"Stale",plan),"Delayed save cannot overwrite newer layout");
        h.assertFalse(data.invite(f,editor,3,UUID.randomUUID(),true),"Editors cannot delegate access");
        f.invited.remove(editor);data.changed(f);h.assertFalse(data.saveLayout(f,editor,f.revision,0,"After revoke",plan),"Revocation is immediate");h.succeed();
    }
    @GameTest(template="empty") public static void invitationsBoundedAndUpdatable(GameTestHelper h) {
        var data=new FarmLedgerData();var owner=UUID.randomUUID();var f=field(data,owner);UUID first=UUID.randomUUID();
        h.assertTrue(data.invite(f,owner,0,first,false),"First invitation accepted");
        for(int i=1;i<8;i++)h.assertTrue(data.invite(f,owner,f.revision,UUID.randomUUID(),false),"Eight allowed");
        h.assertFalse(data.invite(f,owner,f.revision,UUID.randomUUID(),true),"Ninth denied");
        h.assertTrue(data.invite(f,owner,f.revision,first,true)&&f.canEdit(first),"Existing role can change at limit");h.succeed();
    }
    @GameTest(template="empty") public static void plansSurviveSaveAndKeepMissingItems(GameTestHelper h) {
        var data=new FarmLedgerData();var owner=UUID.randomUUID();var guest=UUID.randomUUID();var f=field(data,owner);
        String[] plan={"minecraft:wheat_seeds","absent_mod:blue_seed","","agri_evolution:iron_seed"};
        data.saveLayout(f,owner,0,3,"Metal bed",plan);data.invite(f,owner,1,guest,true);f.machines.add(new BlockPos(3,4,1));
        f.actual[0]="minecraft:wheat";f.flags[0]=FarmLedgerService.KNOWN|FarmLedgerService.SOIL;f.observed[0]=12345;f.surveyedAt=12345;
        var restored=FarmLedgerData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()).fields.get(f.id);
        h.assertTrue(restored!=null&&restored.canEdit(guest)&&Arrays.equals(restored.layouts[3].seeds,plan)&&restored.machines.equals(f.machines)
                &&restored.observed[0]==12345&&restored.actual[0].equals("minecraft:wheat"),"World round-trip preserves plans, roles, missing IDs, links and observations");
        h.succeed();
    }
    @GameTest(template="empty") public static void layoutCopyRequiresAccessAndExactShape(GameTestHelper h) {
        var d=new FarmLedgerData();var owner=UUID.randomUUID();var f=field(d,owner);var other=field(d,owner);
        d.saveLayout(f,owner,0,1,"Wheat",new String[]{"minecraft:wheat_seeds","","",""});
        h.assertTrue(d.copyLayout(other,owner,0,2,f,1),"Same-size copy accepted");
        other.layouts[2].seeds[0]="minecraft:carrot";h.assertTrue(f.layouts[1].seeds[0].equals("minecraft:wheat_seeds"),"Copied arrays do not alias");
        var narrow=d.create(owner,"minecraft:overworld",BlockPos.ZERO,new BlockPos(3,0,0));
        h.assertFalse(d.copyLayout(narrow,owner,0,0,f,1),"Equal area with different shape rejected");
        var outsider=UUID.randomUUID();h.assertFalse(d.copyLayout(other,outsider,other.revision,0,f,1),"Private source cannot leak");h.succeed();
    }
    @GameTest(template="empty") public static void malformedPlansAndNamesRejected(GameTestHelper h) {
        var d=new FarmLedgerData();var owner=UUID.randomUUID();var f=field(d,owner);
        h.assertFalse(d.saveLayout(f,owner,0,4,"Bad",new String[4]),"Fifth slot rejected");
        h.assertFalse(d.saveLayout(f,owner,0,0,"Bad",new String[]{"bad id","","",""}),"Invalid registry ID rejected");
        h.assertFalse(d.saveLayout(f,owner,0,0,"\n",new String[]{"","","",""}),"Control names rejected");
        var tag=FarmLedgerData.strings(new String[]{"minecraft:wheat_seeds","","",""});tag.putIntArray("Cells",new int[]{999,0,0,0});
        h.assertTrue(FarmLedgerData.readStrings(tag,4)==null,"Invalid palette index rejected");
        tag.putIntArray("Cells",new int[1025]);h.assertTrue(FarmLedgerData.readStrings(tag,1025)==null,"Oversized layout rejected");h.succeed();
    }
    @GameTest(template="empty") public static void carriedCountsIncludeBagAndOffhandWithoutChangingGenes(GameTestHelper h) {
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var inv=p.getInventory();
        var seed=new ItemStack(AEItems.IRON_SEED.get(),9);seed.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Iron line"));
        var bag=new ItemStack(AEItems.SEED_BAG.get());bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(seed.copy())));
        inv.setItem(0,new ItemStack(AEItems.IRON_SEED.get(),3));inv.setItem(1,bag);inv.setItem(40,new ItemStack(AEItems.IRON_SEED.get(),2));
        var before=bag.copy();var counts=FarmLedgerService.carriedSeeds(p);
        h.assertTrue(counts.get("agri_evolution:iron_seed")==14&&ItemStack.matches(before,bag),"Count loose, bag and offhand seeds without mutation");h.succeed();
    }
    @GameTest(template="empty") public static void surveyClassifiesSoilCropAndUnknownWithoutHarvesting(GameTestHelper h) {
        var level=h.getLevel();var soil=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(soil,Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(soil.above(),Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE,7));
        var d=new FarmLedgerData();var f=d.create(UUID.randomUUID(),level.dimension().location().toString(),soil,soil);f.layouts[0].seeds[0]="minecraft:wheat_seeds";
        FarmLedgerService.sample(level,f,0);h.assertTrue(FarmLedgerService.cellStatus(f,0,0).equals("Correct")&&(f.flags[0]&FarmLedgerService.MATURE)!=0,"Mature wheat correctly surveyed");
        h.assertTrue(level.getBlockState(soil.above()).getValue(net.minecraft.world.level.block.CropBlock.AGE)==7,"Read-only survey never harvests");
        f.layouts[0].seeds[0]="minecraft:carrot";h.assertTrue(FarmLedgerService.cellStatus(f,0,0).equals("Wrong plant"),"Wrong crop detected");
        f.layouts[0].seeds[0]="absent_mod:seed";h.assertTrue(FarmLedgerService.cellStatus(f,0,0).equals("Unknown"),"Missing mapping is not a mismatch");h.succeed();
    }
    @GameTest(template="empty") public static void nativeAndAgriMappingsIncludeAttachedStems(GameTestHelper h) {
        h.assertTrue(FarmLedgerService.matches("minecraft:melon_seeds","minecraft:attached_melon_stem"),"Attached melon accepted");
        h.assertTrue(FarmLedgerService.matches("minecraft:pumpkin_seeds","minecraft:attached_pumpkin_stem"),"Attached pumpkin accepted");
        h.assertFalse(FarmLedgerService.matches("minecraft:melon_seeds","minecraft:attached_pumpkin_stem"),"Other stem not accepted");
        h.assertTrue(FarmLedgerService.plantedBlock("agri_evolution:iron_seed")==AEBlocks.IRON_CROP.get(),"Agri mapping uses registered crop");
        h.assertTrue(FarmLedgerService.plantedBlock("minecraft:diamond")==null,"Nonseed has no mapping");h.succeed();
    }
    @GameTest(template="empty") public static void unloadedSurveyDoesNotGenerateChunks(GameTestHelper h) {
        var level=h.getLevel();var pos=new BlockPos(25000000,80,25000000);h.assertFalse(level.hasChunkAt(pos),"Fixture is unloaded");
        var f=new FarmLedgerData().create(UUID.randomUUID(),level.dimension().location().toString(),pos,pos);f.layouts[0].seeds[0]="minecraft:wheat_seeds";
        FarmLedgerService.sample(level,f,0);h.assertTrue(f.flags[0]==0&&f.observed[0]==0&&!level.hasChunkAt(pos),"Unloaded cells remain unknown and do not generate chunks");h.succeed();
    }
    @GameTest(template="empty") public static void twoPlayersCannotForgeOrReplayLedgerEdits(GameTestHelper h) {
        var owner=h.makeMockServerPlayerInLevel();var editor=h.makeMockServerPlayerInLevel();
        var d=FarmLedgerData.get(h.getLevel());var f=field(d,owner.getUUID());
        owner.getInventory().setItem(0,new ItemStack(AEItems.FARM_LEDGER.get()));owner.getInventory().selected=0;
        editor.getInventory().setItem(0,new ItemStack(AEItems.FARM_LEDGER.get()));editor.getInventory().selected=0;
        owner.containerMenu=new FarmLedgerMenu(72,owner.getInventory());editor.containerMenu=new FarmLedgerMenu(73,editor.getInventory());
        var input=new CompoundTag();input.putString("Name","Forged");
        FarmLedgerService.request(editor,new FarmLedgerPayload.Request(73,"rename",f.id,0,input));h.assertTrue(f.name.equals("New field"),"Uninvited request cannot rename");
        d.invite(f,owner.getUUID(),f.revision,editor.getUUID(),true);int rev=f.revision;
        input.putString("Name","Bed");input.put("Plan",FarmLedgerData.strings(new String[]{"minecraft:wheat_seeds","","",""}));
        FarmLedgerService.request(editor,new FarmLedgerPayload.Request(999,"save",f.id,rev,input));h.assertTrue(f.revision==rev,"Wrong menu ID rejected");
        FarmLedgerService.request(editor,new FarmLedgerPayload.Request(73,"save",f.id,rev,input));h.assertTrue(f.revision==rev+1,"Invited editor can save");
        input.putString("Name","Replay");FarmLedgerService.request(editor,new FarmLedgerPayload.Request(73,"save",f.id,rev,input));h.assertTrue(f.layouts[0].name.equals("Bed"),"Delayed save rejected");
        f.invited.remove(editor.getUUID());d.changed(f);FarmLedgerService.request(editor,new FarmLedgerPayload.Request(73,"save",f.id,f.revision,input));h.assertTrue(f.layouts[0].name.equals("Bed"),"Revoked editor rejected");
        d.fields.remove(f.id);d.sessions.remove(owner.getUUID());d.sessions.remove(editor.getUUID());d.setDirty();h.succeed();
    }
    @GameTest(template="empty") public static void idleSurveyDoesNoWork(GameTestHelper h) {
        var level=h.getLevel();var d=FarmLedgerData.get(level);var f=field(d,UUID.randomUUID());f.flags[0]=123;
        h.assertTrue(FarmLedgerService.survey(level)==0&&f.flags[0]==123,"Idle ledger scans zero cells");d.fields.remove(f.id);d.setDirty();h.succeed();
    }
    @GameTest(template="empty") public static void surveyBudgetSharedFairlyBetweenFields(GameTestHelper h) {
        var level=h.getLevel();var d=new FarmLedgerData();var owner=UUID.randomUUID();var first=h.absolutePos(new BlockPos(1,2,1));
        var a=d.create(owner,level.dimension().location().toString(),first,first.offset(31,0,31));
        var b=d.create(owner,level.dimension().location().toString(),first.offset(40,0,0),first.offset(71,0,31));
        int used=FarmLedgerService.surveyFields(level,List.of(a,b));
        h.assertTrue(used==128&&a.cursor==64&&b.cursor==64,"All viewers share a single 128-cell budget with fair field progress");h.succeed();
    }
    @GameTest(template="empty") public static void fieldsOutsideDimensionOrRangeStaySnapshots(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();var d=new FarmLedgerData();var here=p.blockPosition().below();
        var f=d.create(p.getUUID(),p.level().dimension().location().toString(),here,here);
        h.assertTrue(FarmLedgerService.nearby(p,f),"Near field may survey");
        var far=d.create(p.getUUID(),f.dimension,here.offset(1000,0,0),here.offset(1000,0,0));
        h.assertFalse(FarmLedgerService.nearby(p,far),"Distant field remains a snapshot");
        var other=d.create(p.getUUID(),"minecraft:the_nether",here,here);h.assertFalse(FarmLedgerService.nearby(p,other),"Other dimension remains a snapshot");h.succeed();
    }
    @GameTest(template="empty") public static void markingAndMenuStopWhenLedgerLeavesHand(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();var held=new ItemStack(AEItems.FARM_LEDGER.get());p.getInventory().selected=0;p.getInventory().setItem(0,held);
        var menu=new FarmLedgerMenu(1,p.getInventory());var d=FarmLedgerData.get(h.getLevel());var s=new FarmLedgerService.Session();s.marking=true;s.markingStack=held;s.corner=p.blockPosition();s.until=Long.MAX_VALUE;d.sessions.put(p.getUUID(),s);
        h.assertTrue(menu.stillValid(p),"Held ledger permits menu");p.getInventory().setItem(0,ItemStack.EMPTY);
        FarmLedgerService.playerTick(p);
        h.assertFalse(menu.stillValid(p)||s.marking||s.until>0,"Putting ledger away cancels selection, menu and preview lease");d.sessions.remove(p.getUUID());h.succeed();
    }
    @GameTest(template="empty") public static void replacedAndUnloadedMachinesAreNotInspected(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(pos,AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState());
        var f=field(new FarmLedgerData(),UUID.randomUUID());f.machines.add(pos);f.machineTypes.add("agri_evolution:water_reservoir");
        FarmLedgerService.machineStatus(level,f);h.assertTrue(f.machineStatus.getFirst().contains("Missing machine"),"A different machine cannot inherit the old physical link");
        var far=new BlockPos(25000000,80,25000000);f.machines.clear();f.machines.add(far);FarmLedgerService.machineStatus(level,f);
        h.assertTrue(f.machineStatus.getFirst().contains("Unloaded")&&!level.hasChunkAt(far),"Machine status never loads a distant chunk");h.succeed();
    }
    @GameTest(template="empty") public static void packetCodecRejectsMalformedAndOversizedData(GameTestHelper h) {
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var tag=FarmLedgerData.strings(new String[]{"absent_mod:seed","","",""});var input=new CompoundTag();input.put("Plan",tag);
            var request=new FarmLedgerPayload.Request(17,"save",UUID.randomUUID(),23,input);FarmLedgerPayload.Request.CODEC.encode(b,request);
            var decoded=FarmLedgerPayload.Request.CODEC.decode(b);h.assertTrue(decoded.menu()==17&&decoded.field().equals(request.field())&&decoded.data().equals(input),"Request survives wire round-trip");
            b.clear();b.writeVarInt(17);b.writeUtf("save");b.writeUUID(request.field());b.writeInt(23);b.writeNbt(StringTag.valueOf("wrong root"));
            boolean rejected=false;try{FarmLedgerPayload.Request.CODEC.decode(b);}catch(RuntimeException expected){rejected=true;}
            h.assertTrue(rejected,"Noncompound packet rejected");
            b.clear();b.writeVarInt(17);b.writeUtf("save");b.writeUUID(request.field());b.writeInt(23);var huge=new CompoundTag();huge.putByteArray("Padding",new byte[270000]);b.writeNbt(huge);
            rejected=false;try{FarmLedgerPayload.Request.CODEC.decode(b);}catch(RuntimeException expected){rejected=true;}
            h.assertTrue(rejected,"Packet NBT allocation is bounded");
        } finally {b.release();}h.succeed();
    }
}
