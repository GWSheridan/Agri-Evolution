package com.agrievolution.content;

import com.agrievolution.farming.BotanicalGreenhouseLayout;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class GreenhouseInteractionGameTests {
    private static BotanicalGreenhouseBlockEntity controller(GameTestHelper h, BlockPos pos, Direction facing) {
        h.getLevel().setBlock(pos, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState()
                .setValue(BotanicalGreenhouseBlock.FACING, facing), 3);
        return (BotanicalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static void use(GameTestHelper h, BlockPos pos, Player player, InteractionHand hand) {
        AEBlocks.BOTANICAL_GREENHOUSE.get().useItemOn(player.getItemInHand(hand), h.getLevel().getBlockState(pos),
                h.getLevel(), pos, player, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static BlockPos doorway(GameTestHelper h, BlockPos pos, Direction facing) {
        var lower = BotanicalGreenhouseLayout.at(pos, facing, 1, 0, 0);
        h.getLevel().setBlock(lower.below(), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(lower, Blocks.BLUE_STAINED_GLASS.defaultBlockState(), 3);
        h.getLevel().setBlock(lower.above(), Blocks.GLASS_PANE.defaultBlockState(), 3);
        return lower;
    }

    private static void fillInventory(Player player) {
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
    }

    @GameTest(template = "empty")
    public static void automationUpgradeRespectsBuildPermission(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(pos));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.GREENHOUSE_AUTOMATION_UPGRADE.get(), 2));
        use(h, pos, player, InteractionHand.MAIN_HAND);
        h.assertTrue(!greenhouse.isAutomationUpgraded() && player.getMainHandItem().getCount() == 2,
                "A denied upgrade leaves the controller and both modules unchanged");
        player.getAbilities().mayBuild = true;
        use(h, pos, player, InteractionHand.MAIN_HAND);
        use(h, pos, player, InteractionHand.MAIN_HAND);
        h.assertTrue(greenhouse.isAutomationUpgraded() && player.getMainHandItem().getCount() == 1,
                "An authorized upgrade uses one module; an installed tier costs nothing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void smartUpgradeRespectsBuildPermission(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        greenhouse.installAutomationUpgrade();
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(pos));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AEItems.GREENHOUSE_SMART_FARMING_UPGRADE.get(), 2));
        use(h, pos, player, InteractionHand.OFF_HAND);
        h.assertTrue(!greenhouse.isSmartFarmingUpgraded() && player.getOffhandItem().getCount() == 2,
                "The smart upgrade uses the same build check in either hand");
        player.getAbilities().mayBuild = true;
        use(h, pos, player, InteractionHand.OFF_HAND);
        h.assertTrue(greenhouse.isSmartFarmingUpgraded() && player.getOffhandItem().getCount() == 1,
                "An authorized offhand module still installs");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void heaterFuelRespectsBuildPermission(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(pos));
        for (var fuel : new net.minecraft.world.item.Item[]{Items.COAL, Items.CHARCOAL}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(fuel, 2));
            use(h, pos, player, InteractionHand.MAIN_HAND);
            h.assertTrue(greenhouse.data.get(16) == 0 && player.getMainHandItem().getCount() == 2,
                    "Denied heater filling preserves fuel and stored heat");
        }
        player.getAbilities().mayBuild = true;
        use(h, pos, player, InteractionHand.MAIN_HAND);
        h.assertTrue(greenhouse.data.get(16) == 600 && player.getMainHandItem().getCount() == 1,
                "A permitted refill gives ten minutes for one charcoal");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void waterTransfersRespectBuildPermission(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        greenhouse.addWater(1000);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(pos));
        for (var hand : InteractionHand.values()) for (var item : new net.minecraft.world.item.Item[]{Items.WATER_BUCKET, Items.BUCKET}) {
            player.setItemInHand(hand, new ItemStack(item));
            use(h, pos, player, hand);
            h.assertTrue(greenhouse.data.get(0) == 1000 && player.getItemInHand(hand).is(item),
                    "Denied fill and drain preserve both water and the held bucket");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorCannotInstallModulesOrFuel(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        var player = h.makeMockPlayer(GameType.SPECTATOR);
        // Explicitly retain build ability to check spectator mode independently.
        player.getAbilities().mayBuild = true;
        player.setPos(Vec3.atCenterOf(pos));
        for (var item : new net.minecraft.world.item.Item[]{AEItems.GREENHOUSE_AUTOMATION_UPGRADE.get(), Items.COAL, Items.WATER_BUCKET}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
            use(h, pos, player, InteractionHand.MAIN_HAND);
        }
        h.assertTrue(!greenhouse.isAutomationUpgraded() && greenhouse.data.get(16) == 0 && greenhouse.data.get(0) == 0,
                "Spectator interactions cannot change machine state even with build ability");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void resourceInteractionsRespectWorldBorder(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var border = h.getLevel().getWorldBorder();
        double oldX = border.getCenterX(), oldZ = border.getCenterZ(), oldSize = border.getSize();
        try {
            border.setCenter(pos.getX() + 100, pos.getZ() + 100);
            border.setSize(10);
            for (var item : new net.minecraft.world.item.Item[]{AEItems.GREENHOUSE_AUTOMATION_UPGRADE.get(), Items.COAL, Items.WATER_BUCKET}) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
                use(h, pos, player, InteractionHand.MAIN_HAND);
                h.assertTrue(player.getMainHandItem().getCount() == 2, "Denied world interaction preserves the held items");
            }
            h.assertTrue(!greenhouse.isAutomationUpgraded() && greenhouse.data.get(16) == 0 && greenhouse.data.get(0) == 0,
                    "World interaction checks protect upgrades, fuel and stored water");
        } finally {
            border.setCenter(oldX, oldZ);
            border.setSize(oldSize);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void stackedBucketsKeepSparesInEitherHand(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        for (var hand : InteractionHand.values()) {
            player.getInventory().clearContent();
            greenhouse.addWater(1000);
            player.setItemInHand(hand, new ItemStack(Items.BUCKET, 16));
            use(h, pos, player, hand);
            h.assertTrue(player.getItemInHand(hand).is(Items.BUCKET) && player.getItemInHand(hand).getCount() == 15
                            && player.getInventory().countItem(Items.WATER_BUCKET) == 1 && greenhouse.data.get(0) == 0,
                    "Draining returns one filled bucket and retains fifteen empty buckets");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyMainHandYieldsToOffhandWaterTransfers(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (var item : new net.minecraft.world.item.Item[]{Items.WATER_BUCKET, Items.BUCKET}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item));
            var result = AEBlocks.BOTANICAL_GREENHOUSE.get().useItemOn(ItemStack.EMPTY, h.getLevel().getBlockState(pos),
                    h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(result == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                    "The empty main hand must yield before the controller opens its screen");
            use(h, pos, player, InteractionHand.OFF_HAND);
        }
        h.assertTrue(greenhouse.data.get(0) == 0 && player.getOffhandItem().is(Items.WATER_BUCKET),
                "The offhand can fill and collect the same bucket");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryDropsFilledBucketWithoutLosingSpares(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        greenhouse.addWater(1000);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        fillInventory(player);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET, 3));
        use(h, pos, player, InteractionHand.OFF_HAND);
        int dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                .filter(entity -> entity.getItem().is(Items.WATER_BUCKET)).mapToInt(entity -> entity.getItem().getCount()).sum();
        h.assertTrue(greenhouse.data.get(0) == 0 && player.getOffhandItem().is(Items.BUCKET)
                        && player.getOffhandItem().getCount() == 2 && dropped == 1,
                "A full inventory drops the filled bucket and keeps the unused empty buckets");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void doorInstallationChecksUpperHalfItemPermission(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        controller(h, pos, Direction.NORTH);
        var lower = doorway(h, pos, Direction.NORTH);
        var player = new Player(h.getLevel(), pos, 0, new GameProfile(UUID.randomUUID(), "door-permission")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
            @Override public boolean mayUseItemAt(BlockPos target, Direction side, ItemStack stack) {
                return !target.equals(lower.above()) && super.mayUseItemAt(target, side, stack);
            }
        };
        player.setPos(Vec3.atCenterOf(pos));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_DOOR, 2));
        use(h, pos, player, InteractionHand.MAIN_HAND);
        h.assertTrue(h.getLevel().getBlockState(lower).is(Blocks.BLUE_STAINED_GLASS)
                        && h.getLevel().getBlockState(lower.above()).is(Blocks.GLASS_PANE)
                        && player.getMainHandItem().getCount() == 2,
                "An upper-half permission denial keeps both glass blocks and the door item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeDoorRetrofitReturnsGlassWithFullInventory(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        controller(h, pos, Direction.NORTH);
        doorway(h, pos, Direction.NORTH);
        var player = h.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        player.setPos(Vec3.atCenterOf(pos));
        fillInventory(player);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_DOOR));
        use(h, pos, player, InteractionHand.OFF_HAND);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3));
        h.assertTrue(BotanicalGreenhouseLayout.hasValidDoor(h.getLevel(), pos, Direction.NORTH)
                        && player.getOffhandItem().is(Items.OAK_DOOR), "A Creative door retrofit still succeeds");
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.BLUE_STAINED_GLASS)).mapToInt(e -> e.getItem().getCount()).sum() == 1
                        && drops.stream().filter(e -> e.getItem().is(Items.GLASS_PANE)).mapToInt(e -> e.getItem().getCount()).sum() == 1,
                "Both removed glass blocks are returned beside a full Creative inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedControllerRejectsDirectResourceMutations(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        greenhouse.addWater(1000);
        greenhouse.setRemoved();
        h.assertFalse(greenhouse.addWater(1000) || greenhouse.removeWater(1000) || greenhouse.addHeaterFuel()
                        || greenhouse.installAutomationUpgrade() || greenhouse.installSmartFarmingUpgrade(),
                "Stale references cannot fill, drain, fuel or upgrade a removed controller");
        h.assertTrue(greenhouse.data.get(0) == 1000 && greenhouse.data.get(16) == 0 && !greenhouse.isAutomationUpgraded(),
                "Rejected mutations preserve detached state");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void waterCapacityAndCreativeBucketsRemainConsistent(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = controller(h, pos, Direction.NORTH);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        greenhouse.addWater(3500);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(h, pos, player, InteractionHand.MAIN_HAND);
        h.assertTrue(greenhouse.data.get(0) == 3500 && player.getMainHandItem().is(Items.WATER_BUCKET), "Partial free space costs no bucket");
        greenhouse.removeWater(3000);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 2));
        use(h, pos, player, InteractionHand.MAIN_HAND);
        h.assertTrue(greenhouse.data.get(0) == 500 && player.getMainHandItem().getCount() == 2, "A partial bucket cannot be withdrawn");
        var creative = h.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        creative.setPos(Vec3.atCenterOf(pos));
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(h, pos, creative, InteractionHand.MAIN_HAND);
        creative.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET, 2));
        use(h, pos, creative, InteractionHand.OFF_HAND);
        h.assertTrue(greenhouse.data.get(0) == 500 && creative.getMainHandItem().is(Items.WATER_BUCKET)
                        && creative.getOffhandItem().getCount() == 2, "Creative transfers preserve held buckets");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void survivalDoorRetrofitWorksInEveryDirection(GameTestHelper h) {
        var origin = h.absolutePos(new BlockPos(4, 220, 4));
        for (var facing : Direction.Plane.HORIZONTAL) {
            var pos = origin.offset(facing.get2DDataValue() * 8, 0, 0);
            controller(h, pos, facing);
            doorway(h, pos, facing);
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(pos));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_DOOR, 2));
            use(h, pos, player, InteractionHand.OFF_HAND);
            h.assertTrue(BotanicalGreenhouseLayout.hasValidDoor(h.getLevel(), pos, facing)
                            && player.getOffhandItem().getCount() == 1
                            && player.getInventory().countItem(Items.BLUE_STAINED_GLASS) == 1
                            && player.getInventory().countItem(Items.GLASS_PANE) == 1,
                    "Successful retrofits consume one door and return both glass blocks: " + facing);
            use(h, pos, player, InteractionHand.OFF_HAND);
            h.assertTrue(player.getOffhandItem().getCount() == 1, "An existing entrance costs no second door");
        }
        h.succeed();
    }
}
