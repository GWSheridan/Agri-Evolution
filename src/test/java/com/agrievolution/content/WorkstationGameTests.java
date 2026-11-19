package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class WorkstationGameTests {
    private static ItemStack studiedSampler(int count) {
        var sampler = new ItemStack(AEItems.SOIL_SAMPLE.get());
        var plots = new ListTag();
        for (int i = 0; i < count; i++) plots.add(StringTag.valueOf("test:plot:" + i));
        var tag = new CompoundTag();
        tag.put("AgriAnalyzedSoil", plots);
        sampler.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return sampler;
    }

    private static BotanicalGeneticsLaboratoryBlockEntity lab(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos, AEBlocks.BOTANICAL_GENETICS_LABORATORY.get().defaultBlockState(), 3);
        return (BotanicalGeneticsLaboratoryBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static void runLab(GameTestHelper h, BotanicalGeneticsLaboratoryBlockEntity lab) {
        for (int i = 0; i < 340 && lab.getItem(4).isEmpty(); i++)
            BotanicalGeneticsLaboratoryBlockEntity.serverTick(h.getLevel(), lab.getBlockPos(), lab.getBlockState(), lab);
    }

    @GameTest(template = "empty")
    public static void greenhouseSettingsRespectBuildPermission(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState(), 3);
        var greenhouse = (BotanicalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
        greenhouse.installAutomationUpgrade();
        greenhouse.installSmartFarmingUpgrade();
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(pos));
        var dashboard = new BotanicalGreenhouseMenu(1, player.getInventory(), greenhouse);
        var seeds = new BotanicalGreenhouseSeedBankMenu(2, player.getInventory(), greenhouse);
        var harvest = new BotanicalGreenhouseHarvestMenu(3, player.getInventory(), greenhouse);
        boolean heater = dashboard.heaterEnabled(), enabled = seeds.slotEnabled(0), exporting = harvest.exporting();
        h.assertFalse(dashboard.clickMenuButton(player, 0), "Visitors cannot change greenhouse heating");
        h.assertFalse(seeds.clickMenuButton(player, 0) || seeds.clickMenuButton(player, 35), "Visitors cannot change seed slots or save presets");
        h.assertFalse(harvest.clickMenuButton(player, 0), "Visitors cannot change harvest export");
        h.assertTrue(heater == dashboard.heaterEnabled() && enabled == seeds.slotEnabled(0)
                && exporting == harvest.exporting(), "Denied actions preserve all three settings");
        player.getAbilities().mayBuild = true;
        h.assertTrue(dashboard.clickMenuButton(player, 0) && seeds.clickMenuButton(player, 0)
                && harvest.clickMenuButton(player, 0), "Builders retain the same working controls");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedGreenhouseRejectsDashboardAndSeedTransfers(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState(), 3);
        var greenhouse = (BotanicalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
        greenhouse.installAutomationUpgrade();
        greenhouse.installSmartFarmingUpgrade();
        greenhouse.seedBank().setItem(0, new ItemStack(AEItems.TOMATO_SEED.get(), 2));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var dashboard = new BotanicalGreenhouseMenu(1, player.getInventory(), greenhouse);
        var seeds = new BotanicalGreenhouseSeedBankMenu(2, player.getInventory(), greenhouse);
        greenhouse.setRemoved();
        h.assertFalse(dashboard.stillValid(player) || seeds.stillValid(player), "Removed controllers invalidate every screen");
        seeds.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(seeds.quickMoveStack(player, 0).isEmpty() && seeds.getCarried().isEmpty()
                && greenhouse.seedBank().getItem(0).getCount() == 2, "Stale screens cannot take seeds before the next menu tick");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacedWorkstationsInvalidateOldInventories(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        for (int machine = 0; machine < 3; machine++) {
            var block = machine == 0 ? AEBlocks.SEED_BREEDING_STATION.get()
                    : machine == 1 ? AEBlocks.SOIL_RESEARCH_TABLE.get() : AEBlocks.BOTANICAL_GENETICS_LABORATORY.get();
            h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
            var old = (BaseContainerBlockEntity) h.getLevel().getBlockEntity(pos);
            AbstractContainerMenu menu = machine == 0 ? new SeedBreedingStationMenu(1, player.getInventory(), (SeedBreedingStationBlockEntity) old)
                    : machine == 1 ? new SoilResearchTableMenu(1, player.getInventory(), (SoilResearchTableBlockEntity) old)
                    : new BotanicalGeneticsLaboratoryMenu(1, player.getInventory(), (BotanicalGeneticsLaboratoryBlockEntity) old);
            old.setItem(4, new ItemStack(Items.DIAMOND, 2));
            h.assertTrue(menu.stillValid(player), "A current nearby workstation remains usable");
            h.getLevel().removeBlockEntity(pos);
            h.getLevel().setBlockEntity(old.getType().create(pos, block.defaultBlockState()));
            h.assertFalse(menu.stillValid(player), "Replacing the same block cannot revive the old menu: " + machine);
            menu.clicked(4, 0, ClickType.PICKUP, player);
            h.assertTrue(menu.getCarried().isEmpty() && menu.quickMoveStack(player, 4).isEmpty()
                    && old.getItem(4).getCount() == 2, "Late pickup and shift-click preserve detached inventories: " + machine);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void researchButtonsRejectDeniedDistantAndSpectatorPlayers(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var lab = lab(h, pos);
        var tablePos = pos.east(2);
        h.getLevel().setBlock(tablePos, AEBlocks.SOIL_RESEARCH_TABLE.get().defaultBlockState(), 3);
        var table = (SoilResearchTableBlockEntity) h.getLevel().getBlockEntity(tablePos);
        table.setItem(0, studiedSampler(8));
        table.setItem(2, new ItemStack(Items.DIRT, 2));
        table.setItem(3, new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 2));
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(pos));
        var labMenu = new BotanicalGeneticsLaboratoryMenu(1, player.getInventory(), lab);
        var tableMenu = new SoilResearchTableMenu(2, player.getInventory(), table);
        h.assertFalse(labMenu.clickMenuButton(player, 1) || tableMenu.clickMenuButton(player, 1), "Research controls reject denied builders");
        player.getAbilities().mayBuild = true;
        player.setPos(Vec3.atCenterOf(pos.above(20)));
        h.assertFalse(labMenu.clickMenuButton(player, 1) || tableMenu.clickMenuButton(player, 1), "Late research clicks reject distant players");
        h.assertTrue(lab.mode() == 0 && table.getItem(2).getCount() == 2 && table.getItem(3).getCount() == 2
                && table.getItem(4).isEmpty(), "Rejected research preserves modes and materials");
        var spectator = h.makeMockPlayer(GameType.SPECTATOR);
        spectator.setPos(Vec3.atCenterOf(pos));
        h.assertFalse(labMenu.clickMenuButton(spectator, 1) || tableMenu.clickMenuButton(spectator, 1), "Spectators cannot operate research controls");
        player.setPos(Vec3.atCenterOf(pos));
        h.assertTrue(labMenu.clickMenuButton(player, 1) && tableMenu.clickMenuButton(player, 1)
                && lab.mode() == 1 && table.getItem(4).is(AEItems.CULTIVATED_FARMLAND.get()), "Authorized research still works");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void researchHoppersRespectInputAndOutputSlots(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var lab = lab(h, pos);
        var tablePos = pos.east(2);
        h.getLevel().setBlock(tablePos, AEBlocks.SOIL_RESEARCH_TABLE.get().defaultBlockState(), 3);
        var table = (SoilResearchTableBlockEntity) h.getLevel().getBlockEntity(tablePos);
        for (var machine : new BaseContainerBlockEntity[]{table, lab}) {
            var stone = new ItemStack(Items.STONE, 3);
            h.assertTrue(HopperBlockEntity.addItem(null, machine, stone, Direction.UP).getCount() == 3,
                    "Hopper insertion rejects unrelated items");
            h.assertFalse(machine.canPlaceItem(4, new ItemStack(AEItems.NUTRIENT_COMPOST.get())), "Hoppers cannot fill the result slot");
        }
        h.assertTrue(HopperBlockEntity.addItem(null, table, new ItemStack(AEItems.NUTRIENT_COMPOST.get()), Direction.UP).isEmpty()
                && table.getItem(3).is(AEItems.NUTRIENT_COMPOST.get()), "The table routes valid catalysts to the catalyst slot");
        h.assertTrue(HopperBlockEntity.addItem(null, lab, new ItemStack(AEItems.NUTRIENT_COMPOST.get()), Direction.UP).isEmpty()
                && lab.getItem(1).is(AEItems.NUTRIENT_COMPOST.get()), "The lab routes compost to its reagent slot");
        var seed = new ItemStack(AEItems.TOMATO_SEED.get());
        h.assertFalse(lab.canPlaceItem(2, seed), "A donor seed requires unlocked hybrid mode");
        lab.installUpgrade();
        lab.installHybridUpgrade();
        lab.selectMode(BotanicalGeneticsLaboratoryBlockEntity.HYBRID);
        h.assertTrue(lab.canPlaceItem(2, seed) && lab.canPlaceItem(2, GeneticSampleItem.capture(seed)), "Unlocked donor slots accept the same inputs as the screen");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void soilResearchUpgradeRespectsBuildPermission(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var block = AEBlocks.SOIL_RESEARCH_TABLE.get();
        h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        var table = (SoilResearchTableBlockEntity) h.getLevel().getBlockEntity(pos);
        table.setItem(0, studiedSampler(72));
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(pos));
        var upgrade = new ItemStack(AEItems.SOIL_RESEARCH_REFINEMENT_UPGRADE.get(), 2);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        block.useItemOn(upgrade, table.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(table.data.get(0) == 1 && upgrade.getCount() == 2, "Denied upgrades preserve the table and module");
        player.getAbilities().mayBuild = true;
        block.useItemOn(upgrade, table.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(table.data.get(0) == 2 && upgrade.getCount() == 1 && table == h.getLevel().getBlockEntity(pos)
                && table.getItem(0).getCount() == 1, "A successful upgrade retains inventory and consumes exactly one module");
        block.useItemOn(upgrade, table.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(upgrade.getCount() == 1, "Repeating an installed tier costs nothing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void laboratoryUpgradeRespectsBuildPermission(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var lab = lab(h, pos);
        var block = AEBlocks.BOTANICAL_GENETICS_LABORATORY.get();
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(pos));
        var upgrade = new ItemStack(AEItems.LABORATORY_RESEARCH_UPGRADE.get(), 2);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        block.useItemOn(upgrade, lab.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(!lab.isUpgraded() && upgrade.getCount() == 2, "Denied research upgrades preserve the module");
        player.getAbilities().mayBuild = true;
        block.useItemOn(upgrade, lab.getBlockState(), h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(lab.isUpgraded() && upgrade.getCount() == 1, "An authorized research upgrade still installs");
        player.getAbilities().mayBuild = false;
        var hybrid = new ItemStack(AEItems.LABORATORY_HYBRIDIZATION_UPGRADE.get());
        block.useItemOn(hybrid, lab.getBlockState(), h.getLevel(), pos, player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(!lab.isHybridUpgraded() && hybrid.getCount() == 1, "The hybrid upgrade has the same permission check");
        player.getAbilities().mayBuild = true;
        block.useItemOn(hybrid, lab.getBlockState(), h.getLevel(), pos, player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(lab.isHybridUpgraded() && hybrid.isEmpty(), "Authorized offhand upgrades remain supported");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void soilResearchReturnsInstalledModulesWhenBroken(GameTestHelper h) {
        for (int tier = 2; tier <= 3; tier++) {
            var pos = h.absolutePos(new BlockPos(4, 200 + tier * 10, 4));
            var block = AEBlocks.SOIL_RESEARCH_TABLE.get();
            h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
            var table = (SoilResearchTableBlockEntity) h.getLevel().getBlockEntity(pos);
            table.setItem(0, studiedSampler(72));
            h.getLevel().setBlock(pos, table.getBlockState().setValue(SoilResearchTableBlock.TIER, tier), 3);
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty(), "Changing a tier does not refund or duplicate modules");
            h.getLevel().destroyBlock(pos, true);
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream().map(ItemEntity::getItem).toList();
            h.assertTrue(drops.stream().filter(s -> s.is(AEItems.SOIL_RESEARCH_REFINEMENT_UPGRADE.get())).mapToInt(ItemStack::getCount).sum() == 1
                    && drops.stream().filter(s -> s.is(AEItems.SOIL_RESEARCH_MASTERY_UPGRADE.get())).mapToInt(ItemStack::getCount).sum() == (tier == 3 ? 1 : 0), "Breaking a table returns each installed module once");
            h.assertTrue(drops.stream().anyMatch(s -> s.is(AEItems.SOIL_SAMPLE.get())
                    && com.agrievolution.farming.SoilResearch.analyzed(s) == 72), "Breaking a table preserves the researched sampler");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void graftsDoNotConsumeMaterialsWithoutAnImprovement(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var lab = lab(h, pos);
        var seed = new ItemStack(AEItems.TOMATO_SEED.get(), 2);
        GenomeStorage.putOnSeed(seed, new CropGenome(100, 100, 100));
        var donor = new ItemStack(AEItems.TOMATO_SEED.get());
        GenomeStorage.putOnSeed(donor, new CropGenome(20, 30, 40));
        lab.setItem(0, seed);
        lab.setItem(1, new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 2));
        lab.setItem(2, GeneticSampleItem.capture(donor));
        lab.setItem(3, new ItemStack(AEItems.IRON_CROP_SUPPORT.get(), 2));
        lab.selectMode(BotanicalGeneticsLaboratoryBlockEntity.GRAFT);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new BotanicalGeneticsLaboratoryMenu(1, player.getInventory(), lab);
        h.assertFalse(menu.graftWouldImprove(), "The screen agrees that the donor cannot improve the seed");
        runLab(h, lab);
        h.assertTrue(lab.getItem(4).isEmpty() && lab.data.get(0) == 0 && seed.getCount() == 2
                && lab.getItem(1).getCount() == 2 && lab.getItem(2).getCount() == 1
                && lab.getItem(3).getCount() == 2, "A graft that cannot improve stats or traits preserves every input");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void graftsStillImproveGenesAndTransferRareTraits(GameTestHelper h) {
        for (int example = 0; example < 2; example++) {
            var pos = h.absolutePos(new BlockPos(4, 210 + example * 10, 4));
            var lab = lab(h, pos);
            var seed = new ItemStack(AEItems.TOMATO_SEED.get());
            var donor = seed.copy();
            GenomeStorage.putOnSeed(seed, new CropGenome(example == 0 ? 40 : 100, 100, 100));
            GenomeStorage.putOnSeed(donor, new CropGenome(90, 90, 90));
            if (example == 1) { lab.installUpgrade(); RareTraitStorage.putOnSeed(donor, RareTrait.VIGOROUS); }
            lab.setItem(0, seed);
            lab.setItem(1, new ItemStack(AEItems.NUTRIENT_COMPOST.get()));
            lab.setItem(2, GeneticSampleItem.capture(donor));
            lab.selectMode(BotanicalGeneticsLaboratoryBlockEntity.GRAFT);
            runLab(h, lab);
            var result = lab.getItem(4);
            h.assertTrue(result.is(AEItems.TOMATO_SEED.get()) && GenomeStorage.fromSeed(result).growth() == (example == 0 ? 90 : 100)
                    && (example == 0 || RareTraitStorage.fromSeed(result) == RareTrait.VIGOROUS), "Useful gene and trait grafts are still allowed");
            h.assertTrue(lab.getItem(0).isEmpty() && lab.getItem(1).isEmpty() && lab.getItem(2).isEmpty(), "Successful grafts charge each input once");
        }
        h.succeed();
    }
}
