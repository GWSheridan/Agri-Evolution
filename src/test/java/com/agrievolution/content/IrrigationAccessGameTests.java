package com.agrievolution.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class IrrigationAccessGameTests {
    private record Rig(int kind, BlockPos part, BlockPos core) {
        int water(ServerLevel level) {
            return switch (kind) {
                case 0 -> ((IrrigationSprinklerBlockEntity) level.getBlockEntity(core)).water();
                case 1 -> level.getBlockState(core).getValue(IrrigationBasinBlock.FILLED) ? 1000 : 0;
                case 2 -> ((WaterReservoirBlockEntity) level.getBlockEntity(core)).water();
                default -> ((VerticalGreenhouseBlockEntity) level.getBlockEntity(core)).water();
            };
        }
    }

    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }

    private static Rig rig(GameTestHelper h, int kind) {
        var core = h.absolutePos(new BlockPos(4 + kind * 8, 220, 4));
        var level = h.getLevel();
        switch (kind) {
            case 0 -> {
                level.setBlock(core, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), 3);
                ((IrrigationSprinklerBlockEntity) level.getBlockEntity(core)).fillBucket();
            }
            case 1 -> level.setBlock(core, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), 3);
            case 2 -> ReservoirGameTests.tank(h, core);
            default -> {
                level.setBlock(core.below(), Blocks.STONE.defaultBlockState(), 3);
                var player = h.makeMockPlayer(GameType.SURVIVAL);
                var item = new ItemStack(AEItems.VERTICAL_GREENHOUSE.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, item);
                item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(core.below())));
                var column = (VerticalGreenhouseBlockEntity) level.getBlockEntity(core);
                h.assertTrue(column != null && column.complete() && column.addBucket(), "The test column is complete and filled");
            }
        }
        return new Rig(kind, kind == 2 ? core.west() : kind == 3 ? core.above() : core, core);
    }

    private static ItemInteractionResult use(GameTestHelper h, Rig rig, Player player, InteractionHand hand) {
        var level = h.getLevel();
        var state = level.getBlockState(rig.part());
        var stack = player.getItemInHand(hand);
        return switch (rig.kind()) {
            case 0 -> AEBlocks.IRRIGATION_SPRINKLER.get().useItemOn(stack, state, level, rig.part(), player, hand, hit(rig.part()));
            case 1 -> AEBlocks.IRRIGATION_BASIN.get().useItemOn(stack, state, level, rig.part(), player, hand, hit(rig.part()));
            case 2 -> AEBlocks.WATER_RESERVOIR.get().useItemOn(stack, state, level, rig.part(), player, hand, hit(rig.part()));
            default -> AEBlocks.VERTICAL_GREENHOUSE.get().useItemOn(stack, state, level, rig.part(), player, hand, hit(rig.part()));
        };
    }

    private static final class AccessPlayer extends Player {
        private final BlockPos denied;
        AccessPlayer(ServerLevel level, BlockPos denied) {
            super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "water-access"));
            this.denied = denied;
        }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            return !pos.equals(denied) && super.mayUseItemAt(pos, side, stack);
        }
    }

    private static void deniedBuckets(GameTestHelper h, int kind) {
        var rig = rig(h, kind);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(rig.part()));
        var items = kind == 3 ? new net.minecraft.world.item.Item[]{Items.WATER_BUCKET}
                : new net.minecraft.world.item.Item[]{Items.BUCKET, Items.WATER_BUCKET};
        for (var hand : InteractionHand.values()) for (var item : items) {
            player.setItemInHand(hand, new ItemStack(item));
            use(h, rig, player, hand);
            h.assertTrue(rig.water(h.getLevel()) == 1000 && player.getItemInHand(hand).is(item),
                    "Denied bucket transfers keep the water and held item: fitting " + kind + ", " + hand);
        }
    }

    @GameTest(template = "empty") public static void sprinklerBucketsRespectBuildPermission(GameTestHelper h) { deniedBuckets(h, 0); h.succeed(); }
    @GameTest(template = "empty") public static void basinBucketsRespectBuildPermission(GameTestHelper h) { deniedBuckets(h, 1); h.succeed(); }
    @GameTest(template = "empty") public static void reservoirBucketsRespectBuildPermission(GameTestHelper h) { deniedBuckets(h, 2); h.succeed(); }
    @GameTest(template = "empty") public static void verticalGreenhouseBucketsRespectBuildPermission(GameTestHelper h) { deniedBuckets(h, 3); h.succeed(); }

    @GameTest(template = "empty")
    public static void reservoirUpgradeRespectsPermissionAndKeepsConduit(GameTestHelper h) {
        var rig = rig(h, 2);
        var player = new AccessPlayer(h.getLevel(), rig.part());
        player.setPos(Vec3.atCenterOf(rig.part()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.CONDUIT, 2));
        use(h, rig, player, InteractionHand.OFF_HAND);
        h.assertTrue(!((WaterReservoirBlockEntity) h.getLevel().getBlockEntity(rig.core())).renewable()
                        && player.getOffhandItem().getCount() == 2,
                "A denied panel upgrade preserves the finite tank and both Conduits");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirPanelCannotBypassCorePermission(GameTestHelper h) {
        var rig = rig(h, 2);
        var player = new AccessPlayer(h.getLevel(), rig.core());
        player.setPos(Vec3.atCenterOf(rig.part()));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(h, rig, player, InteractionHand.MAIN_HAND);
        h.assertTrue(rig.water(h.getLevel()) == 1000 && player.getMainHandItem().is(Items.WATER_BUCKET),
                "An accessible panel cannot fill a protected core");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void upperGreenhouseSectionCannotBypassCorePermission(GameTestHelper h) {
        var rig = rig(h, 3);
        var player = new AccessPlayer(h.getLevel(), rig.core());
        player.setPos(Vec3.atCenterOf(rig.part()));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(h, rig, player, InteractionHand.MAIN_HAND);
        h.assertTrue(rig.water(h.getLevel()) == 1000 && player.getMainHandItem().is(Items.WATER_BUCKET),
                "An accessible upper section cannot fill a protected base tank");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fittingBucketsRespectItemUsePermission(GameTestHelper h) {
        for (int kind = 0; kind < 4; kind++) {
            var rig = rig(h, kind);
            var player = new AccessPlayer(h.getLevel(), rig.part());
            player.setPos(Vec3.atCenterOf(rig.part()));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            use(h, rig, player, InteractionHand.MAIN_HAND);
            h.assertTrue(rig.water(h.getLevel()) == 1000 && player.getMainHandItem().is(Items.WATER_BUCKET),
                    "Item-use denial keeps the tank and bucket unchanged: " + kind);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyMainHandYieldsToOffhandWaterActions(GameTestHelper h) {
        for (int kind = 1; kind < 4; kind++) {
            var rig = rig(h, kind);
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(rig.part()));
            for (var item : kind == 2 ? new net.minecraft.world.item.Item[]{Items.WATER_BUCKET, Items.BUCKET, Items.CONDUIT}
                    : kind == 3 ? new net.minecraft.world.item.Item[]{Items.WATER_BUCKET}
                    : new net.minecraft.world.item.Item[]{Items.WATER_BUCKET, Items.BUCKET}) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item));
                h.assertTrue(use(h, rig, player, InteractionHand.MAIN_HAND) == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                        "Empty main hand yields to supported offhand actions: " + kind + ", " + item);
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldBasinSnapshotCannotUndoAnUpgrade(GameTestHelper h) {
        var rig = rig(h, 1);
        var old = h.getLevel().getBlockState(rig.core());
        var basin = (IrrigationBasinBlockEntity) h.getLevel().getBlockEntity(rig.core());
        h.assertTrue(basin.upgradeTo(1), "Install the copper tier before the old drain arrives");
        h.assertFalse(IrrigationBasinBlock.setFilled(h.getLevel(), rig.core(), old, false), "An old basin state cannot commit a drain");
        h.assertTrue(h.getLevel().getBlockState(rig.core()).getValue(IrrigationBasinBlock.UPGRADE) == 1
                        && rig.water(h.getLevel()) == 1000 && basin.upgradeTier() == 1,
                "The upgrade and its stored water survive a stale drain");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void valveToggleRespectsPermissionInEitherHand(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var block = AEBlocks.IRRIGATION_VALVE.get();
        h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        var player = new AccessPlayer(h.getLevel(), pos);
        player.setPos(Vec3.atCenterOf(pos));
        var original = h.getLevel().getBlockState(pos);
        block.useWithoutItem(original, h.getLevel(), pos, player, hit(pos));
        for (var hand : InteractionHand.values()) {
            player.setItemInHand(hand, new ItemStack(Items.STICK));
            block.useItemOn(player.getItemInHand(hand), h.getLevel().getBlockState(pos), h.getLevel(), pos, player, hand, hit(pos));
        }
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(IrrigationValveBlock.OPEN) == original.getValue(IrrigationValveBlock.OPEN),
                "Denied empty-hand and held-item toggles leave the valve unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldValveClickCannotRestoreAReplacedFitting(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var block = AEBlocks.IRRIGATION_VALVE.get();
        h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        var old = h.getLevel().getBlockState(pos);
        h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        block.useWithoutItem(old, h.getLevel(), pos, h.makeMockPlayer(GameType.SURVIVAL), hit(pos));
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE), "A late toggle cannot restore the removed valve");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void basinCanBeUpgradedFromTheOffhand(GameTestHelper h) {
        var rig = rig(h, 1);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COPPER_BLOCK, 2));
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.OFF_HAND, rig.part(), hit(rig.part()));
        IrrigationBasinUpgradeHandler.onRightClick(event);
        h.assertTrue(event.isCanceled() && ((IrrigationBasinBlockEntity) h.getLevel().getBlockEntity(rig.core())).upgradeTier() == 1
                        && player.getOffhandItem().getCount() == 1 && rig.water(h.getLevel()) == 1000,
                "One offhand copper block upgrades the basin without losing water or placing the spare block");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorCannotUpgradeBasinsOrUseWater(GameTestHelper h) {
        var rig = rig(h, 1);
        var player = h.makeMockPlayer(GameType.SPECTATOR);
        player.getAbilities().mayBuild = true;
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COPPER_BLOCK, 2));
        IrrigationBasinUpgradeHandler.onRightClick(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, rig.part(), hit(rig.part())));
        h.assertTrue(((IrrigationBasinBlockEntity) h.getLevel().getBlockEntity(rig.core())).upgradeTier() == 0
                        && player.getMainHandItem().getCount() == 2, "Spectator upgrades preserve the basin and materials");
        player.setShiftKeyDown(false);
        for (int kind = 0; kind < 4; kind++) {
            var fitting = kind == 1 ? rig : rig(h, kind);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            use(h, fitting, player, InteractionHand.MAIN_HAND);
            h.assertTrue(fitting.water(h.getLevel()) == 1000 && player.getMainHandItem().is(Items.WATER_BUCKET),
                    "Spectator water transfers are read-only: " + kind);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerRejectsSpectatorsAndOtherDimensions(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState(), 3);
        var controller = (IrrigationControllerBlockEntity) h.getLevel().getBlockEntity(pos);
        var spectator = h.makeMockPlayer(GameType.SPECTATOR);
        spectator.getAbilities().mayBuild = true;
        spectator.setPos(Vec3.atCenterOf(pos));
        var menu = new IrrigationControllerMenu(1, spectator.getInventory(), controller);
        h.assertFalse(menu.clickMenuButton(spectator, 5 * IrrigationControllerBlockEntity.ZONES), "Spectators cannot toggle the controller");
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(other != null, "The server has a Nether level for the dimension guard test");
        var remote = new AccessPlayer(other, BlockPos.ZERO);
        remote.setPos(Vec3.atCenterOf(pos));
        h.assertFalse(menu.stillValid(remote) || menu.clickMenuButton(remote, 4 * IrrigationControllerBlockEntity.ZONES),
                "Matching coordinates in another dimension cannot operate the old controller");
        h.assertTrue(controller.enabled() && controller.reserveWater() == 0, "Denied controls preserve the schedule and reserve");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void authorizedWaterAndControllerInteractionsStillWork(GameTestHelper h) {
        for (int kind = 0; kind < 4; kind++) {
            var rig = rig(h, kind);
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(rig.part()));
            if (kind == 1) {
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET, 2));
                use(h, rig, player, InteractionHand.OFF_HAND);
                h.assertTrue(rig.water(h.getLevel()) == 0 && player.getOffhandItem().getCount() == 1
                                && player.getInventory().countItem(Items.WATER_BUCKET) == 1,
                        "An authorized drain keeps the spare empty bucket");
            }
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
            use(h, rig, player, InteractionHand.OFF_HAND);
            h.assertTrue(player.getOffhandItem().is(Items.BUCKET)
                            && (kind == 1 ? rig.water(h.getLevel()) == 1000 : rig.water(h.getLevel()) > 1000),
                    "Authorized offhand fills return a bucket and add stored water: " + kind);
        }
        var pos = h.absolutePos(new BlockPos(4, 225, 4));
        h.getLevel().setBlock(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState(), 3);
        var controller = (IrrigationControllerBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var menu = new IrrigationControllerMenu(1, player.getInventory(), controller);
        h.assertTrue(menu.clickMenuButton(player, 4 * IrrigationControllerBlockEntity.ZONES)
                        && controller.reserveWater() == 1000 && menu.clickMenuButton(player, 5 * IrrigationControllerBlockEntity.ZONES)
                        && !controller.enabled(), "A nearby authorized player can still change the reserve and controller power");
        h.succeed();
    }
}
