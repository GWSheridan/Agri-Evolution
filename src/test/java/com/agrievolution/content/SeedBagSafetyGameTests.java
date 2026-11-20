package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.RegisterEvent;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SeedBagSafetyGameTests {
    private static final ResourceLocation SEED = ResourceLocation.fromNamespaceAndPath("agri_evolution", "test_bag_seed");
    private static Probe activeProbe;
    private static final class Probe {
        int calls;
        final int trigger;
        final Runnable callback;
        Probe(int trigger, Runnable callback) { this.trigger = trigger; this.callback = callback; }
    }
    @EventBusSubscriber(modid = "agri_evolution", bus = EventBusSubscriber.Bus.MOD)
    public static final class TestSeedRegistry {
        @SubscribeEvent public static void register(RegisterEvent event) {
            event.register(Registries.ITEM, SEED, () -> new BlockItem(Blocks.WHEAT, new Item.Properties()) {
                @Override public Block getBlock() {
                    var probe = activeProbe;
                    if (probe != null && ++probe.calls == probe.trigger) probe.callback.run();
                    return super.getBlock();
                }
            });
        }
    }
    private static final class BagPlayer extends Player {
        boolean spectator;
        BagPlayer(GameTestHelper h) {
            super(h.getLevel(), BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "bag-transfer"));
            setPos(h.absolutePos(new BlockPos(4, 220, 4)).getCenter());
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return spectator; }
        void changeWorld(Level level) { setLevel(level); }
    }
    private record Rig(BagPlayer player, ItemStack bag, ItemStack source, SeedBagMenu menu, int bagSlot) {}
    private static ItemStack seed(int count) {
        var seed = new ItemStack(BuiltInRegistries.ITEM.get(SEED), count);
        seed.set(DataComponents.CUSTOM_NAME, Component.literal("Test seed line"));
        GenomeStorage.putOnSeed(seed, new CropGenome(81, 72, 63));
        return seed;
    }
    private static ItemStack bag(ItemStack contents) {
        var bag = new ItemStack(AEItems.SEED_BAG.get());
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contents)));
        CustomData.update(DataComponents.CUSTOM_DATA, bag, tag -> tag.putInt("BagTrial", 7));
        return bag;
    }
    private static Rig rig(GameTestHelper h, boolean empty, boolean offhand) {
        var player = new BagPlayer(h);
        var source = seed(8);
        var bag = bag(empty ? ItemStack.EMPTY : source.copyWithCount(3));
        int bagSlot = offhand ? 40 : 0;
        player.getInventory().setItem(bagSlot, bag);
        player.getInventory().setItem(9, source);
        return new Rig(player, bag, source, new SeedBagMenu(1, player.getInventory(), bagSlot), bagSlot);
    }
    private static int portions(ItemStack bag) {
        return bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().mapToInt(ItemStack::getCount).sum();
    }
    private static void interrupted(GameTestHelper h, int mutation) {
        var r = rig(h, false, false);
        var expectedSource = new ItemStack[1];
        var expectedBag = new ItemStack[1];
        activeProbe = new Probe(1, () -> {
            switch (mutation) {
                case 0 -> r.player().getInventory().setItem(0, bag(new ItemStack(Items.WHEAT_SEEDS, 7)));
                case 1 -> {
                    r.player().getInventory().setItem(0, ItemStack.EMPTY);
                    r.player().getInventory().setItem(40, r.bag());
                }
                case 2 -> r.bag().set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.WHEAT_SEEDS, 7))));
                case 3 -> r.menu().removed(r.player());
                case 4 -> r.player().getInventory().setItem(9, new ItemStack(Items.DIAMOND, 7));
                case 5 -> r.source().grow(5);
                case 6 -> r.source().set(DataComponents.CUSTOM_NAME, Component.literal("Changed seed line"));
                case 7 -> GenomeStorage.putOnSeed(r.source(), new CropGenome(12, 34, 56));
                case 8 -> r.player().setHealth(0);
                case 9 -> r.player().spectator = true;
                case 10 -> r.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                case 11 -> r.source().shrink(6);
                default -> throw new AssertionError("Unknown seed query interruption");
            }
            expectedSource[0] = r.player().getInventory().getItem(9).copy();
            expectedBag[0] = r.bag().copy();
        });
        ItemStack result;
        try { result = r.menu().quickMoveStack(r.player(), 9); } finally { activeProbe = null; }
        h.assertTrue(expectedSource[0] != null && result.isEmpty(), "The seed query ran and interrupted the old deposit");
        h.assertTrue(ItemStack.matches(expectedSource[0], r.player().getInventory().getItem(9)) && ItemStack.matches(expectedBag[0], r.bag()),
                "Interrupted deposits preserve the newer inventory source, bag contents and seed data");
        h.succeed();
    }
    @GameTest(template = "empty") public static void seedQueryBagReplacementCancelsDeposit(GameTestHelper h) { interrupted(h, 0); }
    @GameTest(template = "empty") public static void movingBagDuringSeedQueryCancelsDeposit(GameTestHelper h) { interrupted(h, 1); }
    @GameTest(template = "empty") public static void newerBagContentsSurviveSeedQuery(GameTestHelper h) { interrupted(h, 2); }
    @GameTest(template = "empty") public static void closingBagDuringSeedQueryCancelsDeposit(GameTestHelper h) { interrupted(h, 3); }
    @GameTest(template = "empty") public static void seedQuerySourceReplacementIsPreserved(GameTestHelper h) { interrupted(h, 4); }
    @GameTest(template = "empty") public static void seedQueryCountIncreaseCancelsDeposit(GameTestHelper h) { interrupted(h, 5); }
    @GameTest(template = "empty") public static void seedQueryNameChangeCancelsDeposit(GameTestHelper h) { interrupted(h, 6); }
    @GameTest(template = "empty") public static void seedQueryGeneChangeCancelsDeposit(GameTestHelper h) { interrupted(h, 7); }
    @GameTest(template = "empty") public static void deathDuringSeedQueryCancelsDeposit(GameTestHelper h) { interrupted(h, 8); }
    @GameTest(template = "empty") public static void spectatorDuringSeedQueryCancelsDeposit(GameTestHelper h) { interrupted(h, 9); }
    @GameTest(template = "empty") public static void worldChangeDuringSeedQueryCancelsDeposit(GameTestHelper h) { interrupted(h, 10); }
    @GameTest(template = "empty") public static void seedQueryCountDecreaseCancelsDeposit(GameTestHelper h) { interrupted(h, 11); }

    private static void nested(GameTestHelper h, int kind) {
        var r = rig(h, false, false);
        var nested = new boolean[1];
        activeProbe = new Probe(1, () -> {
            if (kind == 0) nested[0] = !r.menu().quickMoveStack(r.player(), 0).isEmpty();
            else if (kind == 1) {
                r.menu().clicked(0, 0, ClickType.PICKUP, r.player());
                nested[0] = !r.menu().getCarried().isEmpty();
            } else nested[0] = r.menu().clickMenuButton(r.player(), kind == 2 ? 1 : 0);
        });
        ItemStack result;
        try { result = r.menu().quickMoveStack(r.player(), 9); } finally { activeProbe = null; }
        h.assertTrue(!result.isEmpty() && !nested[0] && portions(r.bag()) == 11 && r.player().getInventory().getItem(9).isEmpty()
                        && r.player().getInventory().getItem(8).isEmpty() && r.menu().getCarried().isEmpty(),
                "Only the original transfer completes; a seed query cannot withdraw, pick up, sort or deposit recursively");
        h.succeed();
    }
    @GameTest(template = "empty") public static void seedQueryCannotWithdrawRecursively(GameTestHelper h) { nested(h, 0); }
    @GameTest(template = "empty") public static void seedQueryCannotPickUpRecursively(GameTestHelper h) { nested(h, 1); }
    @GameTest(template = "empty") public static void seedQueryCannotSortRecursively(GameTestHelper h) { nested(h, 2); }
    @GameTest(template = "empty") public static void seedQueryCannotRunDepositAllRecursively(GameTestHelper h) { nested(h, 3); }

    private static void directInsertion(GameTestHelper h, int mutation) {
        var r = rig(h, true, false);
        var expected = new ItemStack[1];
        activeProbe = new Probe(1, () -> {
            if (mutation == 0) r.menu().removed(r.player());
            else if (mutation == 1) r.player().getInventory().setItem(0, bag(new ItemStack(Items.WHEAT_SEEDS, 7)));
            else if (mutation == 2) r.source().grow(5);
            else GenomeStorage.putOnSeed(r.source(), new CropGenome(12, 34, 56));
            expected[0] = r.source().copy();
        });
        try { r.menu().getSlot(0).safeInsert(r.source()); } finally { activeProbe = null; }
        h.assertTrue(expected[0] != null && ItemStack.matches(expected[0], r.source()) && portions(r.bag()) == 0,
                "A direct insertion rechecks the bag and input after modded seed inspection");
        h.succeed();
    }
    @GameTest(template = "empty") public static void directSeedInsertionStopsAfterMenuClose(GameTestHelper h) { directInsertion(h, 0); }
    @GameTest(template = "empty") public static void directSeedInsertionStopsAfterBagReplacement(GameTestHelper h) { directInsertion(h, 1); }
    @GameTest(template = "empty") public static void directSeedInsertionPreservesNewerCount(GameTestHelper h) { directInsertion(h, 2); }
    @GameTest(template = "empty") public static void directSeedInsertionPreservesNewerGenes(GameTestHelper h) { directInsertion(h, 3); }

    @GameTest(template = "empty") public static void moddedSeedIsInspectedOnlyOncePerDeposit(GameTestHelper h) {
        var r = rig(h, true, false);
        var probe = new Probe(Integer.MAX_VALUE, () -> {});
        activeProbe = probe;
        try { r.menu().quickMoveStack(r.player(), 9); } finally { activeProbe = null; }
        h.assertTrue(probe.calls == 1 && portions(r.bag()) == 8, "An approved shift-click does not query the modded seed again while moving it");
        h.succeed();
    }
    @GameTest(template = "empty") public static void seedBagPreviewCanPopulateWithoutPlayer(GameTestHelper h) {
        var inventory = new Inventory(null);
        var menu = new SeedBagMenu(1, inventory, -1);
        menu.getSlot(0).set(new ItemStack(Items.WHEAT_SEEDS, 3));
        h.assertTrue(menu.getSlot(0).getItem().getCount() == 3 && inventory.isEmpty(), "The world-free preview can populate its display without writing a held item");
        h.succeed();
    }
    @GameTest(template = "empty") public static void seedBagPreviewRejectsControlButtons(GameTestHelper h) {
        var menu = new SeedBagMenu(1, new Inventory(null), -1);
        h.assertTrue(!menu.clickMenuButton(null, 0) && !menu.clickMenuButton(null, 1), "Preview controls require a real player");
        h.succeed();
    }
    @GameTest(template = "empty") public static void seedBagPreviewCannotTransferWithoutPlayer(GameTestHelper h) {
        var inventory = new Inventory(null);
        var menu = new SeedBagMenu(1, inventory, -1);
        menu.getSlot(0).set(new ItemStack(Items.WHEAT_SEEDS, 3));
        menu.clicked(0, 0, ClickType.PICKUP, null);
        h.assertTrue(menu.quickMoveStack(null, 0).isEmpty() && menu.getCarried().isEmpty() && menu.getSlot(0).getItem().getCount() == 3,
                "A preview cannot move supplies through playerless click handlers");
        h.succeed();
    }
    @GameTest(template = "empty") public static void nullSeedBagButtonSenderIsRejected(GameTestHelper h) {
        var r = rig(h, false, false);
        h.assertTrue(!r.menu().clickMenuButton(null, 0) && portions(r.bag()) == 3, "A missing button sender cannot run deposit controls");
        h.succeed();
    }

    @GameTest(template = "empty") public static void ordinaryModdedSeedDepositKeepsComponents(GameTestHelper h) {
        var r = rig(h, true, false);
        var expected = r.source().copy();
        h.assertTrue(!r.menu().quickMoveStack(r.player(), 9).isEmpty(), "A compatible modded seed still deposits");
        r.menu().clickMenuButton(r.player(), 1);
        var stored = r.bag().get(DataComponents.CONTAINER).nonEmptyStream().findFirst().orElseThrow();
        h.assertTrue(ItemStack.matches(expected, stored), "Sorting preserves the seed's name, count and genetic data");
        h.succeed();
    }
    @GameTest(template = "empty") public static void nativeSeedsStillDepositAndWithdraw(GameTestHelper h) {
        var r = rig(h, true, false);
        r.player().getInventory().setItem(9, new ItemStack(Items.WHEAT_SEEDS, 8));
        r.menu().quickMoveStack(r.player(), 9);
        r.menu().quickMoveStack(r.player(), 0);
        h.assertTrue(portions(r.bag()) == 0 && r.player().getInventory().countItem(Items.WHEAT_SEEDS) == 8, "Native seeds retain normal deposits and withdrawals");
        h.succeed();
    }
    @GameTest(template = "empty") public static void adventureOffhandBagKeepsNormalTransfers(GameTestHelper h) {
        var r = rig(h, true, true);
        r.player().getAbilities().mayBuild = false;
        r.menu().quickMoveStack(r.player(), 9);
        r.menu().clicked(0, 40, ClickType.SWAP, r.player());
        h.assertTrue(portions(r.bag()) == 8 && r.player().getOffhandItem() == r.bag(), "Personal storage works without build permission and locks the open offhand bag");
        r.menu().quickMoveStack(r.player(), 0);
        h.assertTrue(portions(r.bag()) == 0 && r.player().getInventory().getItem(8).getCount() == 8, "Offhand withdrawal retains all seeds");
        h.succeed();
    }
    @GameTest(template = "empty") public static void fullSeedBagLeavesOverflowInInventory(GameTestHelper h) {
        var r = rig(h, true, false);
        for (int i = 0; i < 9; i++) r.menu().getSlot(i).set(new ItemStack(Items.WHEAT_SEEDS, 64));
        var expected = r.source().copy();
        h.assertTrue(r.menu().quickMoveStack(r.player(), 9).isEmpty() && ItemStack.matches(expected, r.source()) && portions(r.bag()) == 576,
                "A full bag leaves an unmergeable seed stack untouched");
        h.succeed();
    }
    @GameTest(template = "empty") public static void differentSeedLinesRemainSeparateAfterSorting(GameTestHelper h) {
        var r = rig(h, false, false);
        var second = r.source().copyWithCount(4);
        GenomeStorage.putOnSeed(second, new CropGenome(12, 34, 56));
        r.player().getInventory().setItem(10, second);
        r.menu().clickMenuButton(r.player(), 0);
        r.menu().clickMenuButton(r.player(), 1);
        var stored = r.bag().get(DataComponents.CONTAINER).nonEmptyStream().toList();
        h.assertTrue(stored.size() == 2 && stored.get(0).getCount() == 11 && stored.get(1).getCount() == 4
                        && GenomeStorage.fromSeed(stored.get(0)).equals(new CropGenome(81, 72, 63))
                        && GenomeStorage.fromSeed(stored.get(1)).equals(new CropGenome(12, 34, 56)), "Different genetic lines never merge or lose their data");
        h.succeed();
    }
    @GameTest(template = "empty") public static void seedBagDepositAllLeavesNonSeedsAlone(GameTestHelper h) {
        var r = rig(h, true, false);
        r.player().getInventory().setItem(10, new ItemStack(Items.DIAMOND, 7));
        r.menu().clickMenuButton(r.player(), 0);
        h.assertTrue(portions(r.bag()) == 8 && r.player().getInventory().getItem(10).getCount() == 7
                        && r.player().getInventory().getItem(10).is(Items.DIAMOND), "Deposit All accepts compatible seeds and retains other supplies");
        h.succeed();
    }
    @GameTest(template = "empty") public static void seedBagRejectsInvalidSlotsAndButtons(GameTestHelper h) {
        var r = rig(h, false, false);
        var before = r.bag().copy();
        h.assertTrue(r.menu().quickMoveStack(r.player(), -1).isEmpty() && r.menu().quickMoveStack(r.player(), 45).isEmpty()
                        && !r.menu().clickMenuButton(r.player(), -1) && !r.menu().clickMenuButton(r.player(), 2)
                        && ItemStack.matches(before, r.bag()), "Out-of-range requests cannot edit a bag");
        h.succeed();
    }
}
