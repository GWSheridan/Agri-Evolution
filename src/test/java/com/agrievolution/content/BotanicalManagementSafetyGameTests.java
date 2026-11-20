package com.agrievolution.content;

import com.agrievolution.farming.BotanicalGreenhouseLayout;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class BotanicalManagementSafetyGameTests {
    private record House(BlockPos pos, BotanicalGreenhouseBlockEntity entity, Player player, ItemStack seed, ItemStack produce) {}

    private static House house(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 1));
        h.getLevel().setBlock(pos, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState()
                .setValue(BotanicalGreenhouseBlock.FACING, Direction.NORTH), 3);
        var entity = (BotanicalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(entity.installAutomationUpgrade() && entity.installSmartFarmingUpgrade() && entity.addWater(1000), "The controller has both upgrades and stored water");
        var seed = new ItemStack(AEItems.TOMATO_SEED.get(), 7);
        GenomeStorage.putOnSeed(seed, new CropGenome(93, 84, 75));
        seed.set(DataComponents.CUSTOM_NAME, Component.literal("Saved planting line"));
        var produce = new ItemStack(AEItems.TOMATO.get(), 7);
        produce.set(DataComponents.CUSTOM_NAME, Component.literal("Saved harvest"));
        entity.seedBank().setItem(0, seed.copy());
        entity.setItem(0, produce.copy());
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        return new House(pos, entity, player, seed, produce);
    }

    private static AbstractContainerMenu menu(House house, int kind, Player owner) {
        return switch (kind) {
            case 0 -> new BotanicalGreenhouseMenu(1, owner.getInventory(), house.entity());
            case 1 -> new BotanicalGreenhouseSeedBankMenu(2, owner.getInventory(), house.entity());
            case 2 -> new BotanicalGreenhouseHarvestMenu(3, owner.getInventory(), house.entity());
            default -> new BotanicalGreenhouseStorageMenu(4, owner.getInventory(), house.entity());
        };
    }

    private static CompoundTag saved(GameTestHelper h, House house) {
        var tag = new CompoundTag();
        house.entity().saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }

    private static void closedControl(GameTestHelper h, int kind) {
        var house = house(h);
        var before = saved(h, house);
        var menu = menu(house, kind, house.player());
        menu.removed(house.player());
        h.assertFalse(menu.clickMenuButton(house.player(), 0), "A closed management screen cannot edit its controller");
        h.assertTrue(before.equals(saved(h, house)), "Rejected controls preserve all settings, stored water and items");
        h.succeed();
    }

    @GameTest(template = "empty") public static void closedBotanicalDashboardRejectsControls(GameTestHelper h) { closedControl(h, 0); }
    @GameTest(template = "empty") public static void closedBotanicalSeedBankRejectsControls(GameTestHelper h) { closedControl(h, 1); }
    @GameTest(template = "empty") public static void closedBotanicalHarvestManagerRejectsControls(GameTestHelper h) { closedControl(h, 2); }

    @GameTest(template = "empty")
    public static void foreignBotanicalViewerCannotEditManagementScreens(GameTestHelper h) {
        var house = house(h);
        var before = saved(h, house);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(house.pos()));
        for (int kind = 0; kind < 3; kind++) h.assertFalse(menu(house, kind, house.player()).clickMenuButton(other, 0), "A nearby player cannot use another viewer's management controls");
        h.assertTrue(before.equals(saved(h, house)), "All three screens retain their settings");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadBotanicalViewerCannotEditManagementScreens(GameTestHelper h) {
        var house = house(h);
        var before = saved(h, house);
        var screens = List.of(menu(house, 0, house.player()), menu(house, 1, house.player()), menu(house, 2, house.player()));
        house.player().setHealth(0);
        for (var screen : screens) h.assertFalse(screen.clickMenuButton(house.player(), 0), "Dead viewers cannot change greenhouse settings");
        h.assertTrue(before.equals(saved(h, house)), "Death handling cannot leave editable old management controls");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorBotanicalScreensAreInvalid(GameTestHelper h) {
        var house = house(h);
        var spectator = h.makeMockPlayer(GameType.SPECTATOR);
        spectator.setPos(Vec3.atCenterOf(house.pos()));
        for (int kind = 0; kind < 4; kind++) h.assertFalse(menu(house, kind, spectator).stillValid(spectator), "Spectators cannot retain usable management or inventory screens");
        h.succeed();
    }

    private static void deniedWithdrawal(GameTestHelper h, int kind, boolean foreign) {
        var house = house(h);
        var menu = menu(house, kind, house.player());
        var player = house.player();
        if (foreign) {
            player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(house.pos()));
        } else menu.removed(player);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty() && menu.quickMoveStack(player, 0).isEmpty(), "Rejected pickup and shift-click leave both inventories alone");
        h.assertTrue(ItemStack.matches(house.entity().seedBank().getItem(0), house.seed())
                        && ItemStack.matches(house.entity().getItem(0), house.produce()), "Named seeds, genetics and harvest counts are preserved");
        h.succeed();
    }

    @GameTest(template = "empty") public static void closedBotanicalStorageCannotWithdraw(GameTestHelper h) { deniedWithdrawal(h, 3, false); }
    @GameTest(template = "empty") public static void foreignBotanicalStorageViewerCannotWithdraw(GameTestHelper h) { deniedWithdrawal(h, 3, true); }
    @GameTest(template = "empty") public static void closedBotanicalSeedBankCannotWithdraw(GameTestHelper h) { deniedWithdrawal(h, 1, false); }
    @GameTest(template = "empty") public static void foreignBotanicalSeedBankViewerCannotWithdraw(GameTestHelper h) { deniedWithdrawal(h, 1, true); }

    @GameTest(template = "empty")
    public static void closedBotanicalSeedBankSlotRejectsDirectInsertion(GameTestHelper h) {
        var house = house(h);
        var menu = menu(house, 1, house.player());
        menu.removed(house.player());
        var source = house.seed().copyWithCount(3);
        menu.getSlot(1).safeInsert(source);
        h.assertTrue(source.getCount() == 3 && house.entity().seedBank().getItem(1).isEmpty(), "A closed Seed Bank cannot consume an insertion stack through an old slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedBotanicalPlayerSlotsCannotTransfer(GameTestHelper h) {
        var house = house(h);
        house.player().getInventory().setItem(9, new ItemStack(Items.APPLE, 4));
        for (int kind : new int[]{1, 3}) {
            var menu = menu(house, kind, house.player());
            menu.removed(house.player());
            int firstPlayer = kind == 1 ? BotanicalGreenhouseBlockEntity.SEED_BANK_SIZE : BotanicalGreenhouseBlockEntity.STORAGE_SIZE;
            h.assertTrue(menu.getSlot(firstPlayer).safeTake(1, 1, house.player()).isEmpty()
                            && house.player().getInventory().getItem(9).getCount() == 4, "Player inventory slots stop transferring with their management screen");
        }
        h.succeed();
    }

    private static void removedSlot(GameTestHelper h, int kind) {
        var house = house(h);
        var menu = menu(house, kind, house.player());
        house.entity().setRemoved();
        h.assertTrue(menu.getSlot(0).safeTake(7, 7, house.player()).isEmpty(), "Old direct slot references cannot withdraw from a removed controller");
        h.assertTrue(ItemStack.matches(house.entity().seedBank().getItem(0), house.seed())
                        && ItemStack.matches(house.entity().getItem(0), house.produce()), "Removed-controller transfers retain the original named stacks");
        h.succeed();
    }

    @GameTest(template = "empty") public static void removedBotanicalSeedSlotCannotWithdraw(GameTestHelper h) { removedSlot(h, 1); }
    @GameTest(template = "empty") public static void removedBotanicalStorageSlotCannotWithdraw(GameTestHelper h) { removedSlot(h, 3); }

    @GameTest(template = "empty")
    public static void foreignBotanicalViewerCannotUseDirectSlots(GameTestHelper h) {
        var house = house(h);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(house.pos()));
        for (int kind : new int[]{1, 3}) h.assertTrue(menu(house, kind, house.player()).getSlot(0).safeTake(7, 7, other).isEmpty(), "Direct slots check the same owning viewer as the screen");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidBotanicalSeedEligibilityCannotReadPresetStatus(GameTestHelper h) {
        var house = house(h);
        h.assertTrue(house.entity().savePlantingPreset(0), "Preset A has a valid saved status");
        var menu = new BotanicalGreenhouseSeedBankMenu(1, house.player().getInventory(), house.entity());
        for (int slot : new int[]{-1, 9, Integer.MAX_VALUE}) h.assertTrue(menu.eligibleSeeds(slot) == 0, "Invalid seed slots cannot alias preset status or another setting");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidBotanicalExclusionSlotsRemainUnavailable(GameTestHelper h) {
        var house = house(h);
        var menu = new BotanicalGreenhouseSeedBankMenu(1, house.player().getInventory(), house.entity());
        for (int slot : new int[]{-1, 9, Integer.MAX_VALUE}) h.assertTrue(menu.exclusions(slot) == 1, "Invalid exclusion slots report unavailable instead of reading a gene or seed count");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidBotanicalPresetIndicesCannotReadSeedCounts(GameTestHelper h) {
        var house = house(h);
        house.entity().seedBank().setItem(8, house.seed().copyWithCount(2));
        var menu = new BotanicalGreenhouseSeedBankMenu(1, house.player().getInventory(), house.entity());
        for (int preset : new int[]{-1, BotanicalGreenhouseSeedBankMenu.PRESET_COUNT, Integer.MAX_VALUE})
            h.assertTrue(!menu.hasPreset(preset) && !menu.presetMatches(preset), "Invalid presets cannot alias eligible seed counts or another preset");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void botanicalManagementPreviewsWorkWithoutPlayer(GameTestHelper h) {
        var inventory = new Inventory(null);
        var dashboard = new BotanicalGreenhouseMenu(1, inventory);
        var bank = new BotanicalGreenhouseSeedBankMenu(2, inventory);
        var harvest = new BotanicalGreenhouseHarvestMenu(3, inventory);
        var storage = new BotanicalGreenhouseStorageMenu(4, inventory);
        h.assertTrue(dashboard.stillValid(null) && bank.stillValid(null) && harvest.stillValid(null) && storage.stillValid(null)
                        && bank.getSlot(0).mayPlace(new ItemStack(AEItems.TOMATO_SEED.get()))
                        && !storage.getSlot(0).mayPlace(new ItemStack(Items.APPLE)), "World-free previews retain typed input and output slots");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void adventureBotanicalStorageAndSeedBankRemainUsable(GameTestHelper h) {
        var house = house(h);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(house.pos()));
        for (int kind : new int[]{1, 3}) {
            var menu = menu(house, kind, player);
            h.assertTrue(!menu.quickMoveStack(player, 0).isEmpty(), "Adventure viewers retain ordinary inventory access");
        }
        for (int kind = 0; kind < 3; kind++) h.assertFalse(menu(house, kind, player).clickMenuButton(player, 0), "Configuration still requires build permission");
        h.assertTrue(player.getInventory().countItem(AEItems.TOMATO_SEED.get()) == 7 && player.getInventory().countItem(AEItems.TOMATO.get()) == 7,
                "Both inventories transfer the original quantities");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignBotanicalCloseLeavesOwnerScreensUsable(GameTestHelper h) {
        var house = house(h);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(house.pos()));
        for (int kind = 0; kind < 4; kind++) {
            var menu = menu(house, kind, house.player());
            menu.removed(other);
            h.assertTrue(menu.stillValid(house.player()), "Another viewer cannot close this screen");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void botanicalInventoryScreensKeepIndependentViewers(GameTestHelper h) {
        var house = house(h);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(house.pos()));
        for (int kind : new int[]{1, 3}) {
            var first = menu(house, kind, house.player());
            var second = menu(house, kind, other);
            first.removed(house.player());
            h.assertTrue(second.stillValid(other) && !second.quickMoveStack(other, 0).isEmpty(), "Closing one screen leaves another player's own screen usable");
        }
        h.succeed();
    }

    private static House workingHouse(GameTestHelper h) {
        var house = house(h);
        int quiet = 18;
        for (var cell : BotanicalGreenhouseLayout.previewCells(house.pos(), Direction.NORTH)) {
            var state = switch (cell.part()) {
                case OPTIONAL_FARMLAND -> Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7);
                case ENTRANCE_OR_GLASS -> Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH)
                        .setValue(DoorBlock.HALF, cell.pos().getY() == house.pos().getY() ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                default -> Blocks.GLASS.defaultBlockState();
            };
            if (cell.part() == BotanicalGreenhouseLayout.PreviewPart.ENTRANCE_OR_GLASS && cell.pos().getY() == house.pos().getY())
                h.getLevel().setBlock(cell.pos().below(), Blocks.STONE.defaultBlockState(), quiet);
            h.getLevel().setBlock(cell.pos(), state, quiet);
        }
        house.entity().refresh();
        h.assertTrue(house.entity().data.get(2) == 1 && house.entity().addHeaterFuel(), "The real glass structure and doorway form a working, fueled greenhouse");
        house.entity().toggleHeater();
        for (int tick = 0; tick < 19; tick++) BotanicalGreenhouseBlockEntity.serverTick(h.getLevel(), house.pos(), house.entity().getBlockState(), house.entity());
        h.assertTrue(house.entity().data.get(0) == 1000 && house.entity().data.get(16) == 600, "The fixture is one current tick short of its one-second charge");
        return house;
    }

    private static void tick(GameTestHelper h, House house, Level level, BlockPos pos, BlockState state) {
        BotanicalGreenhouseBlockEntity.serverTick(level, pos, state, house.entity());
    }

    private static void uncharged(GameTestHelper h, House house) {
        h.assertTrue(house.entity().data.get(0) == 1000 && house.entity().data.get(16) == 600,
                "Rejected ticks cannot spend greenhouse water or heater fuel");
    }

    @GameTest(template = "empty")
    public static void botanicalGreenhouseTickRejectsWrongWorld(GameTestHelper h) {
        var house = workingHouse(h);
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(other != null, "The Nether exists for this dimension check");
        tick(h, house, other, house.pos(), house.entity().getBlockState());
        uncharged(h, house);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void botanicalGreenhouseTickRejectsWrongPosition(GameTestHelper h) {
        var house = workingHouse(h);
        tick(h, house, h.getLevel(), house.pos().above(), house.entity().getBlockState());
        uncharged(h, house);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void botanicalGreenhouseTickRejectsOldFacingAndResumes(GameTestHelper h) {
        var house = workingHouse(h);
        var old = house.entity().getBlockState();
        var current = old.setValue(BotanicalGreenhouseBlock.FACING, Direction.SOUTH);
        h.getLevel().setBlock(house.pos(), current, 18);
        tick(h, house, h.getLevel(), house.pos(), old);
        uncharged(h, house);
        tick(h, house, h.getLevel(), house.pos(), current);
        h.assertTrue(house.entity().data.get(0) == 999 && house.entity().data.get(16) == 599, "The next current tick performs exactly one second of work");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedBotanicalGreenhouseTickCannotSpendWaterOrFuel(GameTestHelper h) {
        var house = workingHouse(h);
        house.entity().setRemoved();
        tick(h, house, h.getLevel(), house.pos(), house.entity().getBlockState());
        uncharged(h, house);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacedBotanicalGreenhouseRejectsOldResourceTick(GameTestHelper h) {
        var house = workingHouse(h);
        var tag = saved(h, house);
        h.getLevel().removeBlockEntity(house.pos());
        var replacement = new BotanicalGreenhouseBlockEntity(house.pos(), h.getLevel().getBlockState(house.pos()));
        replacement.loadAdditional(tag, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(replacement);
        tick(h, house, h.getLevel(), house.pos(), house.entity().getBlockState());
        uncharged(h, house);
        h.assertTrue(replacement.data.get(0) == 1000 && replacement.data.get(16) == 600, "The replacement's saved water and fuel are untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentBotanicalGreenhouseTickKeepsExactResourceCost(GameTestHelper h) {
        var house = workingHouse(h);
        tick(h, house, h.getLevel(), house.pos(), house.entity().getBlockState());
        h.assertTrue(house.entity().data.get(0) == 999 && house.entity().data.get(16) == 599,
                "A current working greenhouse uses one mB and one heater-fuel second");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void validBotanicalSeedStatusAndPresetsStillWork(GameTestHelper h) {
        var house = house(h);
        var menu = new BotanicalGreenhouseSeedBankMenu(1, house.player().getInventory(), house.entity());
        h.assertTrue(menu.eligibleSeeds(0) == 7 && menu.exclusions(0) == 0 && !menu.hasPreset(0), "Valid seed counts and exclusions keep their original values");
        h.assertTrue(menu.clickMenuButton(house.player(), BotanicalGreenhouseSeedBankMenu.SAVE_PRESET_START)
                        && menu.hasPreset(0) && menu.presetMatches(0), "Preset A can still be saved and reports matching rules");
        h.succeed();
    }
}
