package com.agrievolution.content;

import com.agrievolution.farming.SoilResearch;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SoilTableSafetyGameTests {
    private record Table(BlockPos pos, SoilResearchTableBlockEntity entity, Player player, SoilResearchTableMenu menu) {}

    private static ItemStack sampler(int count, int pending) {
        var sampler = new ItemStack(AEItems.SOIL_SAMPLE.get());
        var tag = new CompoundTag();
        var entries = new ListTag();
        for (int i = 0; i < count; i++) entries.add(StringTag.valueOf("test:studied:" + i));
        tag.put("AgriAnalyzedSoil", entries);
        var credits = new CompoundTag();
        credits.putInt("dirt", pending);
        tag.put("AgriPendingSpecimens", credits);
        sampler.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return sampler;
    }

    private static Table table(GameTestHelper h, GameType mode, int tier, int studied) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().removeBlock(pos, false);
        h.getLevel().setBlock(pos, AEBlocks.SOIL_RESEARCH_TABLE.get().defaultBlockState().setValue(SoilResearchTableBlock.TIER, tier), 2);
        var entity = (SoilResearchTableBlockEntity) h.getLevel().getBlockEntity(pos);
        entity.setItem(0, sampler(studied, 8));
        h.getLevel().setBlock(pos.below(3), Blocks.DIRT.defaultBlockState(), 2);
        entity.setItem(1, SoilSpecimenItem.from(h.getLevel(), pos.below(3)).copyWithCount(8));
        entity.setItem(2, new ItemStack(Items.DIRT, 5));
        entity.setItem(3, new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 5));
        var player = h.makeMockPlayer(mode);
        player.setPos(Vec3.atCenterOf(pos));
        return new Table(pos, entity, player, new SoilResearchTableMenu(1, player.getInventory(), entity));
    }

    private static Table table(GameTestHelper h) { return table(h, GameType.SURVIVAL, 1, 8); }

    private static void untouched(GameTestHelper h, Table table) {
        h.assertTrue(SoilResearch.analyzed(table.entity().getItem(0)) == 8 && table.entity().getItem(1).getCount() == 8
                        && table.entity().getItem(2).getCount() == 5 && table.entity().getItem(3).getCount() == 5
                        && table.entity().getItem(4).isEmpty(), "Rejected research keeps the sampler, specimens, soil, catalysts and output");
    }

    @GameTest(template = "empty")
    public static void closedSoilTableCannotPickUpSampler(GameTestHelper h) {
        var table = table(h);
        table.menu().removed(table.player());
        table.menu().clicked(0, 0, ClickType.PICKUP, table.player());
        h.assertTrue(table.menu().getCarried().isEmpty(), "A closed screen cannot pick up the sampler");
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedSoilTableCannotAnalyze(GameTestHelper h) {
        var table = table(h);
        table.menu().removed(table.player());
        h.assertFalse(table.menu().clickMenuButton(table.player(), 0), "Closed screens cannot analyze another specimen");
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedSoilTableCannotRefine(GameTestHelper h) {
        var table = table(h);
        table.menu().removed(table.player());
        h.assertFalse(table.menu().clickMenuButton(table.player(), 1), "Closed screens cannot start refinement");
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignViewerCannotAnalyzeSoilTable(GameTestHelper h) {
        var table = table(h);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(table.pos()));
        h.assertFalse(table.menu().clickMenuButton(other, 0), "Another nearby player cannot use this viewer's research buttons");
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorCannotWithdrawSoilTableInputs(GameTestHelper h) {
        var table = table(h, GameType.SPECTATOR, 1, 8);
        table.menu().clicked(0, 0, ClickType.PICKUP, table.player());
        h.assertTrue(table.menu().getCarried().isEmpty() && table.menu().quickMoveStack(table.player(), 0).isEmpty(),
                "Spectators cannot withdraw through pickup or shift-click");
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadViewerCannotWithdrawSoilTableInputs(GameTestHelper h) {
        var table = table(h);
        table.player().setHealth(0);
        h.assertTrue(table.menu().quickMoveStack(table.player(), 0).isEmpty(), "A dead viewer cannot withdraw through the old screen");
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadPlayerCannotDirectlyAnalyzeSoilTable(GameTestHelper h) {
        var table = table(h);
        table.player().setHealth(0);
        table.entity().analyze(table.player());
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadPlayerCannotDirectlyRefineSoilTable(GameTestHelper h) {
        var table = table(h);
        table.player().setHealth(0);
        table.entity().refine(table.player());
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedSoilTableSlotCannotBeTakenDirectly(GameTestHelper h) {
        var table = table(h);
        table.entity().setRemoved();
        h.assertTrue(table.menu().getSlot(0).safeTake(1, 1, table.player()).isEmpty(), "A retained slot cannot withdraw from a removed table");
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedSoilTableSlotsRejectDirectTransfers(GameTestHelper h) {
        var table = table(h);
        table.player().getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
        table.menu().removed(table.player());
        var offered = new ItemStack(Items.DIRT, 2);
        table.menu().getSlot(2).safeInsert(offered);
        h.assertTrue(offered.getCount() == 2 && table.menu().getSlot(32).safeTake(1, 1, table.player()).isEmpty(),
                "Machine and player slots both enforce closed-screen access");
        untouched(h, table);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedSoilTableRejectsInputAndStatusCalls(GameTestHelper h) {
        var table = table(h);
        table.entity().setRemoved();
        table.entity().setStatus(13);
        h.assertTrue(!table.entity().canPlaceItem(0, table.entity().getItem(0))
                        && !table.entity().canPlaceItem(2, new ItemStack(Items.DIRT)) && table.entity().data.get(2) == 0,
                "Removed tables do not advertise valid inputs or accept late status changes");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void adventureSoilTableInventoryWorksWithoutResearch(GameTestHelper h) {
        var table = table(h, GameType.ADVENTURE, 1, 8);
        table.player().getAbilities().mayBuild = false;
        h.assertTrue(!table.menu().quickMoveStack(table.player(), 0).isEmpty()
                        && !table.menu().clickMenuButton(table.player(), 1)
                        && table.player().getInventory().countItem(AEItems.SOIL_SAMPLE.get()) == 1,
                "Ordinary inventory access remains available while research still requires permission");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void soilTablePreviewWorksWithoutPlayer(GameTestHelper h) {
        var menu = new SoilResearchTableMenu(1, new Inventory(null));
        h.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(AEItems.SOIL_SAMPLE.get()))
                        && menu.getSlot(2).mayPlace(new ItemStack(Items.DIRT))
                        && !menu.getSlot(4).mayPlace(new ItemStack(Items.DIRT)), "World-free previews retain typed input and output slots");
        h.succeed();
    }

    private static final class UpgradePlayer extends Player {
        boolean allow = true;
        Runnable hook;
        UpgradePlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "soil-upgrade"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            var callback = hook;
            hook = null;
            if (callback != null) callback.run();
            return allow && super.mayUseItemAt(pos, side, stack);
        }
    }

    private static ItemStack module(int tier) {
        return new ItemStack(tier == 2 ? AEItems.SOIL_RESEARCH_REFINEMENT_UPGRADE.get() : AEItems.SOIL_RESEARCH_MASTERY_UPGRADE.get(), 2);
    }

    private static ItemInteractionResult upgrade(GameTestHelper h, Table table, Player player, InteractionHand hand, ItemStack module, BlockState state) {
        return AEBlocks.SOIL_RESEARCH_TABLE.get().useItemOn(module, state, h.getLevel(), table.pos(), player, hand,
                new BlockHitResult(Vec3.atCenterOf(table.pos()), Direction.UP, table.pos(), false));
    }

    @GameTest(template = "empty")
    public static void soilTableUpgradesRespectItemUsePermission(GameTestHelper h) {
        for (int tier = 2; tier <= 3; tier++) {
            var table = table(h, GameType.SURVIVAL, tier - 1, 96);
            var player = new UpgradePlayer(h.getLevel(), table.pos());
            var module = module(tier);
            player.setItemInHand(InteractionHand.MAIN_HAND, module);
            player.allow = false;
            h.assertTrue(upgrade(h, table, player, InteractionHand.MAIN_HAND, module, table.entity().getBlockState()) == ItemInteractionResult.FAIL
                            && table.entity().data.get(0) == tier - 1 && module.getCount() == 2,
                    "Denied item use preserves both upgrade tiers and their modules");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedHeldModuleCancelsSoilTableUpgrade(GameTestHelper h) {
        for (int tier = 2; tier <= 3; tier++) {
            var table = table(h, GameType.SURVIVAL, tier - 1, 96);
            var player = new UpgradePlayer(h.getLevel(), table.pos());
            var module = module(tier);
            player.setItemInHand(InteractionHand.MAIN_HAND, module);
            player.hook = () -> player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            upgrade(h, table, player, InteractionHand.MAIN_HAND, module, table.entity().getBlockState());
            h.assertTrue(table.entity().data.get(0) == tier - 1 && module.getCount() == 2 && player.getMainHandItem().is(Items.STICK),
                    "Changed held items cancel the old upgrade without consuming the module");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedModuleDataCancelsSoilTableUpgrade(GameTestHelper h) {
        var table = table(h, GameType.SURVIVAL, 1, 96);
        var player = new UpgradePlayer(h.getLevel(), table.pos());
        var module = module(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, module);
        player.hook = () -> module.set(DataComponents.CUSTOM_NAME, Component.literal("Changed module"));
        upgrade(h, table, player, InteractionHand.MAIN_HAND, module, table.entity().getBlockState());
        h.assertTrue(table.entity().data.get(0) == 1 && module.getCount() == 2
                        && Component.literal("Changed module").equals(module.get(DataComponents.CUSTOM_NAME)), "Module component changes cancel installation and retain the changed item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacementSoilTableDoesNotInheritOldUpgrade(GameTestHelper h) {
        var table = table(h, GameType.SURVIVAL, 1, 96);
        var player = new UpgradePlayer(h.getLevel(), table.pos());
        var module = module(2);
        var state = table.entity().getBlockState();
        player.setItemInHand(InteractionHand.MAIN_HAND, module);
        player.hook = () -> {
            h.getLevel().removeBlockEntity(table.pos());
            h.getLevel().setBlockEntity(new SoilResearchTableBlockEntity(table.pos(), state));
        };
        upgrade(h, table, player, InteractionHand.MAIN_HAND, module, state);
        var replacement = h.getLevel().getBlockEntity(table.pos());
        h.assertTrue(replacement != table.entity() && h.getLevel().getBlockState(table.pos()) == state && module.getCount() == 2,
                "Replacing the table during a permission check cancels its old upgrade click");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldSoilTableStateCannotDemoteMasteryTable(GameTestHelper h) {
        var table = table(h, GameType.SURVIVAL, 3, 96);
        var module = module(2);
        table.player().setItemInHand(InteractionHand.MAIN_HAND, module);
        var current = table.entity().getBlockState();
        var old = current.setValue(SoilResearchTableBlock.TIER, 1);
        upgrade(h, table, table.player(), InteractionHand.MAIN_HAND, module, old);
        h.assertTrue(h.getLevel().getBlockState(table.pos()) == current && module.getCount() == 2,
                "An old Mk I state cannot downgrade a Mk III table and consume another refinement module");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rotatedSoilTableCancelsOldUpgrade(GameTestHelper h) {
        var table = table(h, GameType.SURVIVAL, 1, 96);
        var player = new UpgradePlayer(h.getLevel(), table.pos());
        var module = module(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, module);
        var old = table.entity().getBlockState();
        var rotated = old.setValue(SoilResearchTableBlock.FACING, Direction.EAST);
        player.hook = () -> h.getLevel().setBlock(table.pos(), rotated, 2);
        upgrade(h, table, player, InteractionHand.MAIN_HAND, module, old);
        h.assertTrue(h.getLevel().getBlockState(table.pos()) == rotated && module.getCount() == 2,
                "A permission callback can rotate the table without the old upgrade restoring its facing or charging a module");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadPlayerCannotUpgradeSoilTable(GameTestHelper h) {
        var table = table(h, GameType.SURVIVAL, 1, 96);
        var module = module(2);
        table.player().setItemInHand(InteractionHand.MAIN_HAND, module);
        table.player().setHealth(0);
        upgrade(h, table, table.player(), InteractionHand.MAIN_HAND, module, table.entity().getBlockState());
        h.assertTrue(table.entity().data.get(0) == 1 && module.getCount() == 2, "Dead players cannot install or spend table upgrades");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandSoilTableUpgradesReachTheUsedHand(GameTestHelper h) {
        for (int tier = 2; tier <= 3; tier++) {
            var table = table(h, GameType.SURVIVAL, tier - 1, 96);
            var module = module(tier);
            table.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            table.player().setItemInHand(InteractionHand.OFF_HAND, module);
            h.assertTrue(upgrade(h, table, table.player(), InteractionHand.MAIN_HAND, ItemStack.EMPTY, table.entity().getBlockState())
                            == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                    "An empty main hand leaves the upgrade click for the offhand");
            upgrade(h, table, table.player(), InteractionHand.OFF_HAND, module, table.entity().getBlockState());
            h.assertTrue(table.entity().data.get(0) == tier && module.getCount() == 1, "Either tier installs and charges one offhand module");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void insufficientResearchPreservesSoilTableModules(GameTestHelper h) {
        for (int tier = 2; tier <= 3; tier++) {
            var table = table(h, GameType.SURVIVAL, tier - 1, tier == 2 ? 47 : 71);
            var module = module(tier);
            table.player().setItemInHand(InteractionHand.MAIN_HAND, module);
            upgrade(h, table, table.player(), InteractionHand.MAIN_HAND, module, table.entity().getBlockState());
            h.assertTrue(table.entity().data.get(0) == tier - 1 && table.entity().data.get(2) == 16 && module.getCount() == 2,
                    "The 48- and 72-analysis unlock requirements remain enforced without charging a failed upgrade");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wrongOrderAndRepeatedSoilTableUpgradesCostNothing(GameTestHelper h) {
        for (int tier : new int[]{1, 3}) {
            var table = table(h, GameType.SURVIVAL, tier, 96);
            var module = module(tier == 1 ? 3 : 2);
            table.player().setItemInHand(InteractionHand.MAIN_HAND, module);
            upgrade(h, table, table.player(), InteractionHand.MAIN_HAND, module, table.entity().getBlockState());
            h.assertTrue(table.entity().data.get(0) == tier && module.getCount() == 2
                            && table.entity().data.get(2) == (tier == 1 ? 15 : 14),
                    "Wrong upgrade order and already-complete tables retain their tier and module");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tableAnalysisRespectsEachTierResearchLimit(GameTestHelper h) {
        for (int tier = 1; tier <= 3; tier++) {
            int limit = tier == 1 ? 48 : tier == 2 ? 72 : 96;
            var table = table(h, GameType.SURVIVAL, tier, limit - 1);
            table.entity().analyze(table.player());
            h.assertTrue(SoilResearch.analyzed(table.entity().getItem(0)) == limit && table.entity().getItem(1).getCount() == 7
                            && table.entity().data.get(3) == 1, "Only the final required specimen is charged at the research cap");
            table.entity().analyze(table.player());
            h.assertTrue(SoilResearch.analyzed(table.entity().getItem(0)) == limit && table.entity().getItem(1).getCount() == 7,
                    "A capped sampler does not consume another specimen");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void masteryRefinementKeepsEverySoilRecipeAndExactCosts(GameTestHelper h) {
        Item[] sources = {Items.DIRT, AEItems.CULTIVATED_FARMLAND.get(), AEItems.NUTRIENT_FARMLAND.get(),
                AEItems.MYTHIC_FARMLAND.get(), AEItems.ARCANE_FARMLAND.get()};
        Item[] catalysts = {AEItems.NUTRIENT_COMPOST.get(), Items.COPPER_INGOT, Items.AMETHYST_SHARD, Items.GOLD_INGOT, Items.ECHO_SHARD};
        Item[] results = {AEItems.CULTIVATED_FARMLAND.get(), AEItems.NUTRIENT_FARMLAND.get(), AEItems.MYTHIC_FARMLAND.get(),
                AEItems.ARCANE_FARMLAND.get(), AEItems.CELESTIAL_FARMLAND.get()};
        for (int recipe = 0; recipe < sources.length; recipe++) {
            var table = table(h, GameType.SURVIVAL, 3, 96);
            table.entity().setItem(2, new ItemStack(sources[recipe], 5));
            table.entity().setItem(3, new ItemStack(catalysts[recipe], 4));
            table.entity().refine(table.player());
            h.assertTrue(table.entity().getItem(4).is(results[recipe]) && table.entity().getItem(4).getCount() == 4
                            && table.entity().getItem(2).getCount() == 1 && table.entity().getItem(3).isEmpty()
                            && SoilResearch.analyzed(table.entity().getItem(0)) == 96,
                    "Every soil recipe retains its result and one-for-one input and catalyst costs");
        }
        h.succeed();
    }
}
