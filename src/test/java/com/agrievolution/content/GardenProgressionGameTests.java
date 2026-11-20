package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.SpeciesBreeder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class GardenProgressionGameTests {
    @GameTest(template = "empty", timeoutTicks = 240)
    public static void compostAcceptsCuttingsAndMatures(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 230, 4));
        var bin = AEBlocks.COMPOST_BIN.get();
        level.setBlock(pos, bin.defaultBlockState(), 3);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var leaves = new ItemStack(Items.OAK_LEAVES, 5);
        var weeds = new ItemStack(AEItems.PULLED_WEEDS.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, leaves);
        for (int i = 0; i < 2; i++) bin.useItemOn(leaves, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        player.setItemInHand(InteractionHand.MAIN_HAND, weeds);
        for (int i = 0; i < 3; i++) bin.useItemOn(weeds, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(pos).getValue(CompostBinBlock.FILL) == CompostBinBlock.FULL
                        && weeds.isEmpty() && leaves.getCount() == 3, "Leaves and weeds fill by their listed amounts");
        player.setItemInHand(InteractionHand.MAIN_HAND, leaves);
        bin.useItemOn(leaves, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(leaves.getCount() == 3 && CompostBinBlock.value(new ItemStack(Items.IRON_INGOT)) == 0,
                "Full bins and unsuitable items are rejected");
        helper.runAfterDelay(CompostBinBlock.FERMENT_TICKS + 3, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(CompostBinBlock.FILL) == CompostBinBlock.READY,
                    "The scheduled block tick finishes compost");
            bin.useWithoutItem(level.getBlockState(pos), level, pos, player, hit);
            bin.useWithoutItem(level.getBlockState(pos), level, pos, player, hit);
            int compost = player.getInventory().items.stream().filter(stack -> stack.is(AEItems.NUTRIENT_COMPOST.get()))
                    .mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(compost == 2 && level.getBlockState(pos).getValue(CompostBinBlock.FILL) == 0, "Each batch pays exactly once");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void seedVaultLocksSurviveReloadAndKeepGenomesSeparate(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 235, 4));
        level.setBlock(pos, AEBlocks.SEED_VAULT.get().defaultBlockState(), 3);
        var vault = (SeedVaultBlockEntity) level.getBlockEntity(pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 1.5);
        var menu = new SeedVaultMenu(1, player.getInventory(), vault);
        var first = new ItemStack(AEItems.CRIMSON_CLOVER_SEED.get(), 2);
        var second = first.copy();
        GenomeStorage.putOnSeed(first, new CropGenome(90, 30, 40));
        GenomeStorage.putOnSeed(second, new CropGenome(40, 90, 30));
        vault.setItem(0, first);
        helper.assertTrue(menu.clickMenuButton(player, 0) && menu.locked(0), "A populated slot locks");
        player.getInventory().setItem(9, first.copy());
        player.getInventory().setItem(10, second);
        menu.quickMoveStack(player, SeedVaultMenu.SIZE);
        menu.quickMoveStack(player, SeedVaultMenu.SIZE + 1);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(vault.getItem(0).getCount() == 2 && vault.getItem(1).getCount() == 2
                        && GenomeStorage.fromSeed(vault.getItem(2)).yield() == 90
                        && menu.getCarried().isEmpty() && !menu.canTakeItemForPickAll(first, menu.getSlot(0)),
                "Locked stacks reject shift inserts and pickup and distinct genomes do not merge");
        var saved = new CompoundTag();
        vault.saveAdditional(saved, level.registryAccess());
        vault.loadAdditional(saved, level.registryAccess());
        helper.assertTrue(menu.locked(0) && GenomeStorage.fromSeed(vault.getItem(0)).growth() == 90
                        && vault.getSlotsForFace(Direction.DOWN).length == 0, "Vault contents and locks persist with no hopper access");
        helper.assertTrue(menu.clickMenuButton(player, 0) && !menu.quickMoveStack(player, 0).isEmpty(), "Unlock restores normal pickup");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void beeVisitAddsTemporaryFieldBonus(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 240, 4));
        level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        var mature = AEBlocks.CRIMSON_CLOVER_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 7);
        level.setBlock(pos, mature.setValue(AgriCropBlock.AGE, 1), 3);
        helper.assertFalse(CropPollination.markVisit(level, pos), "Immature crops cannot receive the breeding bonus");
        level.setBlock(pos, mature, 3);
        var bee = EntityType.BEE.create(level);
        var nectar = new CompoundTag();
        nectar.putBoolean("HasNectar", true);
        nectar.put("flower_pos", NbtUtils.writeBlockPos(pos));
        bee.readAdditionalSaveData(nectar);
        bee.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        CropPollination.beeVisit(new EntityTickEvent.Post(bee));
        var crop = (AECropBlockEntity) level.getBlockEntity(pos);
        long time = level.getGameTime();
        helper.assertTrue(crop.pollinated(time) && !crop.pollinated(time + CropPollination.WINDOW), "A visit grants a five-minute window");
        crop.markPollinated(time);
        CropPollination.beeVisit(new EntityTickEvent.Post(bee));
        helper.assertFalse(crop.pollinated(time), "One nectar trip cannot keep renewing its bonus");
        long seed = 0;
        while (new java.util.Random(seed).nextInt(100) != 31) seed++;
        helper.assertTrue(SpeciesBreeder.cross(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, seed, 5, 0) != CropSpecies.VIOLET_STARFLOWER
                        && SpeciesBreeder.cross(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, seed, 5, CropPollination.BONUS) == CropSpecies.VIOLET_STARFLOWER,
                "The field bonus adds to the support bonus");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void milestonesAwardThroughAdvancements(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var milestones = helper.getLevel().getServer().getAdvancements();
        for (var id : new String[]{"first_cross", "study_soil", "form_reservoir", "first_preserve", "vertical_greenhouse"}) {
            var milestone = milestones.get(ResourceLocation.fromNamespaceAndPath("agri_evolution", "farming/" + id));
            helper.assertTrue(milestone != null && milestone.value().criteria().containsKey("achieved"), "Milestone data loaded for " + id);
        }
        FarmingMilestones.SOIL.award(player);
        var soil = milestones.get(ResourceLocation.fromNamespaceAndPath("agri_evolution", "farming/study_soil"));
        helper.assertTrue(player.getAdvancements().getOrStartProgress(soil).isDone(), "Milestone awards the matching advancement");
        helper.succeed();
    }
}
