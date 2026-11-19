package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SeedVaultAccessGameTests {
    private record Vault(BlockPos pos, Player player, SeedVaultBlockEntity entity, SeedVaultMenu menu, ItemStack seed) {}

    private static Vault vault(GameTestHelper h, GameType mode) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.SEED_VAULT.get().defaultBlockState(), 2);
        var entity = (SeedVaultBlockEntity) h.getLevel().getBlockEntity(pos);
        var seed = new ItemStack(AEItems.TOMATO_SEED.get(), 7);
        GenomeStorage.putOnSeed(seed, new CropGenome(81, 72, 93));
        entity.setItem(0, seed.copy());
        var player = h.makeMockPlayer(mode);
        player.setPos(Vec3.atCenterOf(pos));
        return new Vault(pos, player, entity, new SeedVaultMenu(1, player.getInventory(), entity), seed);
    }

    @GameTest(template = "empty")
    public static void foreignPlayerCannotPickUpVaultContents(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(vault.pos()));
        vault.menu().clicked(0, 0, ClickType.PICKUP, other);
        h.assertTrue(vault.menu().getCarried().isEmpty() && ItemStack.matches(vault.entity().getItem(0), vault.seed()),
                "A foreign player's click cannot take another viewer's vault contents");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignPlayerCannotChangeVaultLocks(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(vault.pos()));
        h.assertFalse(vault.menu().clickMenuButton(other, 0), "A lock action belongs to the menu's actual viewer");
        h.assertTrue(vault.entity().locks() == 0, "Rejected lock actions keep the slot mask unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorCannotWithdrawVaultSeeds(GameTestHelper h) {
        var vault = vault(h, GameType.SPECTATOR);
        vault.menu().clicked(0, 0, ClickType.PICKUP, vault.player());
        h.assertTrue(vault.menu().getCarried().isEmpty() && vault.menu().quickMoveStack(vault.player(), 0).isEmpty()
                        && ItemStack.matches(vault.entity().getItem(0), vault.seed()),
                "Spectator pickup and shift-click cannot withdraw the saved seed line");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadPlayerCannotShiftClickVaultSeeds(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.player().setHealth(0);
        h.assertTrue(vault.menu().quickMoveStack(vault.player(), 0).isEmpty()
                        && ItemStack.matches(vault.entity().getItem(0), vault.seed()),
                "A dead viewer cannot withdraw through the old vault screen");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anotherDimensionCannotSatisfyVaultAccess(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        var otherWorld = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(otherWorld != null && otherWorld != h.getLevel(), "The fixture has a separate dimension");
        var player = new Player(otherWorld, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "vault-world")) {
            @Override public boolean isCreative() { return false; }
            @Override public boolean isSpectator() { return false; }
        };
        player.setPos(Vec3.atCenterOf(vault.pos()));
        var menu = new SeedVaultMenu(2, player.getInventory(), vault.entity());
        h.assertTrue(!vault.entity().stillValid(player) && !menu.stillValid(player),
                "Matching coordinates in another dimension do not grant vault access");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedVaultRejectsLatePickup(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.menu().removed(vault.player());
        vault.menu().clicked(0, 0, ClickType.PICKUP, vault.player());
        h.assertTrue(vault.menu().getCarried().isEmpty() && ItemStack.matches(vault.entity().getItem(0), vault.seed()),
                "Closing permanently disables the old vault pickup path");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedVaultRejectsLateLockActions(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.menu().removed(vault.player());
        h.assertTrue(!vault.menu().clickMenuButton(vault.player(), 0) && vault.entity().locks() == 0,
                "A late lock click cannot act after the screen has closed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVaultSlotCannotBeTakenDirectly(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.entity().setRemoved();
        var taken = vault.menu().getSlot(0).safeTake(1, 1, vault.player());
        h.assertTrue(taken.isEmpty() && ItemStack.matches(vault.entity().getItem(0), vault.seed()),
                "A direct slot reference cannot withdraw from a removed vault");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVaultSlotCannotAcceptSeedsDirectly(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.entity().setRemoved();
        var offered = vault.seed().copyWithCount(2);
        vault.menu().getSlot(1).safeInsert(offered);
        h.assertTrue(offered.getCount() == 2 && vault.entity().getItem(1).isEmpty(),
                "A removed vault rejects direct insertion without consuming the offered seeds");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedVaultSlotCannotAcceptSeedsDirectly(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.menu().removed(vault.player());
        var offered = vault.seed().copyWithCount(2);
        vault.menu().getSlot(1).safeInsert(offered);
        h.assertTrue(offered.getCount() == 2 && vault.entity().getItem(1).isEmpty(),
                "A closed menu's slot reference cannot save seeds into the vault");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVaultIgnoresDirectSettingChanges(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.entity().setRemoved();
        vault.entity().setIntakeSpecies(CropSpecies.TOMATO);
        vault.entity().cycleMinimum(0);
        vault.entity().toggleLock(0);
        h.assertTrue(vault.entity().intakeSpeciesId() == 0 && vault.entity().minimum(0) == 0 && vault.entity().locks() == 0,
                "Detached vault references cannot change filters or slot locks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidVaultSlotAddressesCannotWrapIntoValidLocks(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.entity().toggleLock(32);
        vault.entity().toggleLock(-32);
        vault.entity().toggleLock(64);
        h.assertTrue(vault.entity().locks() == 0 && !vault.entity().canPlaceItem(-1, vault.seed())
                        && !vault.entity().canPlaceItem(27, vault.seed()) && !vault.entity().canPlaceItem(32, vault.seed()),
                "Invalid indices neither wrap their bit shifts into slot zero nor accept inserts");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidVaultTraitAddressesLeaveFiltersAlone(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.entity().cycleMinimum(-1);
        vault.entity().cycleMinimum(3);
        vault.entity().cycleMinimum(Integer.MAX_VALUE);
        h.assertTrue(vault.entity().minimum(0) == 0 && vault.entity().minimum(1) == 0 && vault.entity().minimum(2) == 0,
                "Invalid trait indices leave the filters unchanged without throwing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVaultRejectsPlacementQueries(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.entity().setRemoved();
        h.assertFalse(vault.entity().canPlaceItem(1, vault.seed()), "Removed inventories do not advertise a valid insertion target");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void adventureVaultInventoryWorksWithoutChangingLocks(GameTestHelper h) {
        var vault = vault(h, GameType.ADVENTURE);
        vault.player().getAbilities().mayBuild = false;
        h.assertFalse(vault.menu().clickMenuButton(vault.player(), 0), "Adventure viewers cannot change the vault configuration");
        h.assertFalse(vault.menu().quickMoveStack(vault.player(), 0).isEmpty(), "Ordinary permitted inventory withdrawals still work");
        h.assertTrue(vault.entity().isEmpty() && vault.player().getInventory().countItem(AEItems.TOMATO_SEED.get()) == 7,
                "The original stack moves exactly once without build permission");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void vaultIntakeKeepsLockedAndDifferentGeneLinesSeparate(GameTestHelper h) {
        var vault = vault(h, GameType.SURVIVAL);
        vault.entity().toggleLock(0);
        var different = vault.seed().copyWithCount(2);
        GenomeStorage.putOnSeed(different, new CropGenome(62, 93, 71));
        h.assertTrue(vault.entity().receiveOffspring(different) && vault.entity().receiveOffspring(vault.seed()),
                "Unlocked slots accept both genetic lines");
        h.assertTrue(ItemStack.matches(vault.entity().getItem(0), vault.seed())
                        && GenomeStorage.fromSeed(vault.entity().getItem(1)).equals(new CropGenome(62, 93, 71))
                        && GenomeStorage.fromSeed(vault.entity().getItem(2)).equals(new CropGenome(81, 72, 93))
                        && vault.entity().getSlotsForFace(Direction.DOWN).length == 0,
                "Intake skips locked slots, preserves separate genomes and keeps hopper access disabled");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void vaultPreviewCanDisplayWithoutAPlayer(GameTestHelper h) {
        var inventory = new Inventory(null);
        var menu = new SeedVaultMenu(1, inventory);
        menu.setData(0, 1);
        menu.getSlot(0).set(new ItemStack(Items.WHEAT_SEEDS, 5));
        h.assertTrue(menu.locked(0) && menu.getSlot(0).getItem().getCount() == 5 && inventory.isEmpty(),
                "The world-free preview displays locks and contents without touching a player inventory");
        h.succeed();
    }
}
