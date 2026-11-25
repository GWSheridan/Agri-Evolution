package com.agrievolution.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SpannerSafetyGameTests {
    private static final class HookPlayer extends Player {
        boolean spectator;
        BlockPos denied, trigger;
        Runnable hook;
        HookPlayer(ServerLevel level) {
            super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "spanner-check"));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return spectator; }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            if (hook != null && (trigger == null || trigger.equals(pos))) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            return !pos.equals(denied) && super.mayUseItemAt(pos, side, stack);
        }
    }

    private record Rig(int kind, BlockPos pos, BlockState state, BlockEntity entity, HookPlayer player, ItemStack tool) {}

    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false); }

    private static Rig rig(GameTestHelper h, int kind) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var level = h.getLevel();
        level.getChunkAt(pos.east());
        var block = switch (kind) {
            case 0 -> AEBlocks.IRRIGATION_PIPE.get();
            case 1 -> AEBlocks.IRRIGATION_SPRINKLER.get();
            case 2 -> AEBlocks.IRRIGATION_VALVE.get();
            default -> AEBlocks.IRRIGATION_CONTROLLER.get();
        };
        level.setBlock(pos, block.defaultBlockState(), 2);
        var entity = level.getBlockEntity(pos);
        if (entity instanceof IrrigationValveBlockEntity valve) valve.rename("Before");
        var player = new HookPlayer(level);
        player.setPos(Vec3.atCenterOf(pos));
        player.setShiftKeyDown(kind >= 2);
        var tool = new ItemStack(AEItems.SPANNER.get());
        tool.set(DataComponents.CUSTOM_NAME, Component.literal("Orchard"));
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return new Rig(kind, pos, level.getBlockState(pos), entity, player, tool);
    }

    private static void use(Rig rig) {
        if (rig.kind() == 3) AEItems.SPANNER.get().useOn(new UseOnContext(rig.player(), InteractionHand.MAIN_HAND, hit(rig.pos())));
        else com.agrievolution.platform.NeoForgeEvents.spannerUse(new PlayerInteractEvent.RightClickBlock(rig.player(), InteractionHand.MAIN_HAND, rig.pos(), hit(rig.pos())));
    }

    private static void unchanged(GameTestHelper h, Rig rig) {
        h.assertTrue(h.getLevel().getBlockState(rig.pos()) == rig.state(), "Rejected Spanner actions keep the block state");
        if (rig.entity() instanceof IrrigationSprinklerBlockEntity head)
            h.assertFalse(head.smartWatering(), "Rejected controls keep continuous watering selected");
        if (rig.entity() instanceof IrrigationValveBlockEntity valve)
            h.assertTrue(valve.zoneName().equals("Before"), "Rejected naming preserves the valve's name");
        h.assertTrue(!rig.tool().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("AgriControllerPos"),
                "A rejected selection does not save a controller into the old tool");
    }

    private static void spectator(GameTestHelper h, int kind) {
        var rig = rig(h, kind);
        rig.player().spectator = true;
        use(rig);
        unchanged(h, rig);
        h.succeed();
    }
    @GameTest(template = "empty") public static void spectatorCannotTogglePipeWithSpanner(GameTestHelper h) { spectator(h, 0); }
    @GameTest(template = "empty") public static void spectatorCannotConfigureSprinklerWithSpanner(GameTestHelper h) { spectator(h, 1); }
    @GameTest(template = "empty") public static void spectatorCannotRenameValveWithSpanner(GameTestHelper h) { spectator(h, 2); }
    @GameTest(template = "empty") public static void spectatorCannotSelectIrrigationController(GameTestHelper h) { spectator(h, 3); }

    @GameTest(template = "empty")
    public static void deadPlayerCannotConfigureSprinklerWithSpanner(GameTestHelper h) {
        var rig = rig(h, 1);
        rig.player().setHealth(0);
        use(rig);
        unchanged(h, rig);
        h.succeed();
    }

    private static void changedTool(GameTestHelper h, int kind) {
        var rig = rig(h, kind);
        rig.player().hook = () -> rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        use(rig);
        unchanged(h, rig);
        h.assertTrue(rig.player().getMainHandItem().is(Items.STICK)
                        && rig.player().getMainHandItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).equals(CustomData.EMPTY),
                "The replacement held item remains unchanged");
        h.succeed();
    }
    @GameTest(template = "empty") public static void changedSpannerCannotTogglePipe(GameTestHelper h) { changedTool(h, 0); }
    @GameTest(template = "empty") public static void changedSpannerCannotConfigureSprinkler(GameTestHelper h) { changedTool(h, 1); }
    @GameTest(template = "empty") public static void changedSpannerCannotRenameValve(GameTestHelper h) { changedTool(h, 2); }
    @GameTest(template = "empty") public static void changedSpannerCannotSelectController(GameTestHelper h) { changedTool(h, 3); }

    @GameTest(template = "empty")
    public static void changedSpannerNameCannotRenameValve(GameTestHelper h) {
        var rig = rig(h, 2);
        rig.player().hook = () -> rig.tool().set(DataComponents.CUSTOM_NAME, Component.literal("Changed during permission check"));
        use(rig);
        unchanged(h, rig);
        h.succeed();
    }

    private static BlockEntity replace(GameTestHelper h, Rig rig) {
        h.getLevel().removeBlockEntity(rig.pos());
        var replacement = rig.entity().getType().create(rig.pos(), rig.state());
        h.getLevel().setBlockEntity(replacement);
        return replacement;
    }

    @GameTest(template = "empty")
    public static void replacedSprinklerCannotReceiveOldSpannerEdit(GameTestHelper h) {
        var rig = rig(h, 1);
        rig.player().hook = () -> replace(h, rig);
        use(rig);
        var replacement = (IrrigationSprinklerBlockEntity) h.getLevel().getBlockEntity(rig.pos());
        h.assertTrue(replacement != rig.entity() && !replacement.smartWatering(), "The new sprinkler does not inherit the old click");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacedValveCannotReceiveOldSpannerName(GameTestHelper h) {
        var rig = rig(h, 2);
        rig.player().hook = () -> ((IrrigationValveBlockEntity) replace(h, rig)).rename("Replacement");
        use(rig);
        h.assertTrue(((IrrigationValveBlockEntity) h.getLevel().getBlockEntity(rig.pos())).zoneName().equals("Replacement"),
                "A replacement valve keeps its own name");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedControllerCannotBeSelectedByOldSpannerClick(GameTestHelper h) {
        var rig = rig(h, 3);
        rig.player().hook = () -> h.getLevel().removeBlock(rig.pos(), false);
        use(rig);
        h.assertTrue(!rig.tool().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("AgriControllerPos"),
                "A permission callback that removes the controller cancels selection");
        h.succeed();
    }

    private static IrrigationControllerBlockEntity selectController(GameTestHelper h, Rig valve) {
        var pos = valve.pos().west(3);
        h.getLevel().setBlock(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState(), 2);
        AEItems.SPANNER.get().useOn(new UseOnContext(valve.player(), InteractionHand.MAIN_HAND, hit(pos)));
        return (IrrigationControllerBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    @GameTest(template = "empty")
    public static void deniedValveLinkKeepsSelectedController(GameTestHelper h) {
        var rig = rig(h, 2);
        var controller = selectController(h, rig);
        var selected = rig.tool().copy();
        rig.player().denied = controller.getBlockPos();
        use(rig);
        h.assertTrue(ItemStack.matches(selected, rig.tool()) && !controller.contains(rig.pos())
                        && ((IrrigationValveBlockEntity) rig.entity()).controller() == null,
                "A denied controller edit keeps the pending selection for a later retry");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacedControllerCannotReceiveOldValveLink(GameTestHelper h) {
        var rig = rig(h, 2);
        var controller = selectController(h, rig);
        var selected = rig.tool().copy();
        rig.player().trigger = controller.getBlockPos();
        rig.player().hook = () -> {
            h.getLevel().removeBlockEntity(controller.getBlockPos());
            h.getLevel().setBlockEntity(new IrrigationControllerBlockEntity(controller.getBlockPos(), controller.getBlockState()));
        };
        use(rig);
        h.assertTrue(ItemStack.matches(selected, rig.tool()) && !controller.contains(rig.pos())
                        && ((IrrigationValveBlockEntity) rig.entity()).controller() == null,
                "Replacing a selected controller during its permission check leaves both machines and the selection alone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacedValveDuringControllerCheckCannotLink(GameTestHelper h) {
        var rig = rig(h, 2);
        var controller = selectController(h, rig);
        rig.player().trigger = controller.getBlockPos();
        rig.player().hook = () -> replace(h, rig);
        use(rig);
        h.assertTrue(!controller.contains(rig.pos()) && ((IrrigationValveBlockEntity) rig.entity()).controller() == null
                        && ((IrrigationValveBlockEntity) h.getLevel().getBlockEntity(rig.pos())).controller() == null,
                "A controller check cannot attach a removed valve or reserve its slot for a replacement");
        h.succeed();
    }

    private static void changedBlueprint(GameTestHelper h, boolean source) {
        var core = h.absolutePos(new BlockPos(4, 220, 4));
        ReservoirGameTests.blueprint(h, core);
        var changed = source ? core : core.east();
        var replacement = (source ? Blocks.AIR : Blocks.DIAMOND_BLOCK).defaultBlockState();
        int[] checks = {0};
        var result = ReservoirStructure.tryForm(h.getLevel(), core.north(), pos -> {
            if (++checks[0] == 27) h.getLevel().setBlock(changed, replacement, 2);
            return true;
        });
        h.assertTrue(result == ReservoirStructure.Formation.INVALID && h.getLevel().getBlockState(changed) == replacement
                        && h.getLevel().getBlockState(core.west()).is(Blocks.COPPER_BLOCK),
                "Formation rechecks the entire blueprint after permission hooks and preserves replacement blocks");
        h.succeed();
    }
    @GameTest(template = "empty") public static void changedCopperShellCancelsReservoirFormation(GameTestHelper h) { changedBlueprint(h, false); }
    @GameTest(template = "empty") public static void removedCentreSourceCancelsReservoirFormation(GameTestHelper h) { changedBlueprint(h, true); }

    @GameTest(template = "empty")
    public static void recursiveReservoirFormationCannotFormTwice(GameTestHelper h) {
        var core = h.absolutePos(new BlockPos(4, 220, 4));
        ReservoirGameTests.blueprint(h, core);
        boolean[] nested = {false}, called = {false};
        var result = ReservoirStructure.tryForm(h.getLevel(), core.north(), pos -> {
            if (!called[0]) {
                called[0] = true;
                nested[0] = ReservoirStructure.tryForm(h.getLevel(), core.north());
            }
            return true;
        });
        h.assertTrue(result == ReservoirStructure.Formation.FORMED && !nested[0]
                        && ((WaterReservoirBlockEntity) h.getLevel().getBlockEntity(core)).water() == 1000,
                "A permission callback cannot run a second formation inside the first one");
        h.succeed();
    }

    private static void formWithPlayer(GameTestHelper h, boolean spectator, boolean changeTool, InteractionHand hand) {
        var core = h.absolutePos(new BlockPos(4, 220, 4));
        ReservoirGameTests.blueprint(h, core);
        var player = new HookPlayer(h.getLevel());
        player.setPos(Vec3.atCenterOf(core.north()));
        player.spectator = spectator;
        player.setItemInHand(hand, new ItemStack(AEItems.SPANNER.get()));
        if (changeTool) player.hook = () -> player.setItemInHand(hand, new ItemStack(Items.STICK));
        AEItems.SPANNER.get().useOn(new UseOnContext(player, hand, hit(core.north())));
        boolean allowed = !spectator && !changeTool;
        h.assertTrue(ReservoirStructure.complete(h.getLevel(), core) == allowed,
                "Only a live, authorized Spanner use can form the reservoir");
        if (allowed) h.assertTrue(((WaterReservoirBlockEntity) h.getLevel().getBlockEntity(core)).water() == 1000
                        && player.getItemInHand(hand).is(AEItems.SPANNER.get()), "Successful formation preserves the tool and starting bucket");
        else h.assertTrue(h.getLevel().getBlockState(core).is(Blocks.WATER)
                        && h.getLevel().getBlockState(core.north()).is(Blocks.COPPER_BLOCK), "Denied formation leaves the blueprint intact");
        h.succeed();
    }
    @GameTest(template = "empty") public static void changedSpannerCancelsReservoirFormation(GameTestHelper h) { formWithPlayer(h, false, true, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void spectatorCannotFormReservoirWithSpanner(GameTestHelper h) { formWithPlayer(h, true, false, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void offhandSpannerStillFormsReservoir(GameTestHelper h) { formWithPlayer(h, false, false, InteractionHand.OFF_HAND); }

    @GameTest(template = "empty")
    public static void unnamedValveLinkStillAllowsNaming(GameTestHelper h) {
        var rig = rig(h, 2);
        var valve = (IrrigationValveBlockEntity) rig.entity();
        valve.rename("");
        var controller = selectController(h, rig);
        use(rig);
        h.assertTrue(!controller.contains(rig.pos()) && valve.controller() == null
                        && !rig.tool().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("AgriControllerPos"),
                "A normal unnamed-valve link attempt releases the selection for naming");
        use(rig);
        h.assertTrue(valve.zoneName().equals("Orchard"), "The next click can name the valve without selecting another controller");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void normalSpannerEditsAndDeniedLinkRetryStillWork(GameTestHelper h) {
        var pipe = rig(h, 0);
        use(pipe);
        h.assertTrue(h.getLevel().getBlockState(pipe.pos()) != pipe.state(), "An ordinary Spanner click toggles the pipe port");
        var head = rig(h, 1);
        use(head);
        h.assertTrue(((IrrigationSprinklerBlockEntity) head.entity()).smartWatering(), "An ordinary click selects dry-soil mode");
        var valve = rig(h, 2);
        use(valve);
        h.assertTrue(((IrrigationValveBlockEntity) valve.entity()).zoneName().equals("Orchard"), "Crouch-use copies the tool's name");
        var controller = selectController(h, valve);
        valve.player().denied = controller.getBlockPos();
        use(valve);
        valve.player().denied = null;
        use(valve);
        h.assertTrue(controller.contains(valve.pos()) && ((IrrigationValveBlockEntity) valve.entity()).controlledBy(controller.getBlockPos()),
                "The same selection can link successfully after permission is restored");
        h.succeed();
    }
}
