package com.agrievolution.content;

import com.agrievolution.client.SoilSamplerClientScreens;
import com.agrievolution.farming.SoilResearch;
import com.agrievolution.farming.SoilSurveyData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Soil research is bound to this sampler stack and unlocks five farmland tiers. */
public final class SoilSampleItem extends Item {
    public SoilSampleItem(Properties properties) { super(properties); }

    private record Plot(BlockPos soil, BlockState soilState, BlockState plantState,
                        BlockEntity soilEntity, BlockEntity plantEntity, CompoundTag cropData) {
        static Plot read(Level level, BlockPos soil) {
            if (!level.hasChunkAt(soil) || level.isOutsideBuildHeight(soil)) return null;
            var plant = level.getBlockEntity(soil.above());
            return new Plot(soil.immutable(), level.getBlockState(soil), level.getBlockState(soil.above()),
                    level.getBlockEntity(soil), plant, plant instanceof AECropBlockEntity crop ? snapshot(level, crop) : null);
        }

        boolean current(Level level) {
            return level.hasChunkAt(soil) && level.getBlockState(soil) == soilState && level.getBlockState(soil.above()) == plantState
                    && level.getBlockEntity(soil) == soilEntity && level.getBlockEntity(soil.above()) == plantEntity
                    && currentEntity(level, soil, soilEntity) && currentEntity(level, soil.above(), plantEntity)
                    && (cropData == null || cropData.equals(snapshot(level, (AECropBlockEntity) plantEntity)));
        }
    }

    private static boolean currentEntity(Level level, BlockPos pos, BlockEntity entity) {
        return entity == null || entity.getLevel() == level && entity.getBlockPos().equals(pos) && WorkstationAccess.isCurrent(entity);
    }

    private static CompoundTag snapshot(Level level, AECropBlockEntity plant) {
        var tag = new CompoundTag();
        plant.saveAdditional(tag, level.registryAccess());
        return tag;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos clicked = context.getClickedPos().immutable();
        ItemStack sampler = context.getItemInHand();
        ItemStack before = sampler.copy();
        boolean area = player.isShiftKeyDown();
        if (sampler.getItem() != this || !currentSample(context, sampler, before, area)
                || !level.hasChunkAt(clicked) || level.isOutsideBuildHeight(clicked)) return InteractionResult.FAIL;
        BlockState clickedState = level.getBlockState(clicked);
        boolean crop = clickedState.getBlock() instanceof AgriCropBlock;
        BlockPos soil = crop ? clicked.below() : clicked;
        var center = Plot.read(level, soil);
        if (center == null) return InteractionResult.FAIL;
        BlockState state = center.soilState();
        boolean farmland = SoilResearch.isSoil(state);
        boolean naturalDirt = state.is(net.minecraft.world.level.block.Blocks.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.PODZOL);
        if (!farmland && !naturalDirt) return InteractionResult.PASS;
        if (!center.current(level) || !maySamplePlot(context, center, sampler, before, area)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            if (farmland) {
                var allowed = new java.util.HashMap<BlockPos, Plot>();
                allowed.put(soil, center);
                if (area) for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
                    BlockPos plot = soil.offset(dx, 0, dz);
                    if (plot.equals(soil) || !level.hasChunkAt(plot)) continue;
                    var checked = Plot.read(level, plot);
                    if (checked == null || !SoilResearch.isSoil(checked.soilState())) continue;
                    if (checked.current(level) && maySamplePlot(context, checked, sampler, before, area)) {
                        allowed.put(plot, checked);
                    }
                    if (!currentSample(context, sampler, before, area) || !center.current(level)) return InteractionResult.FAIL;
                }
                // Gather permission decisions before changing the held tool or issuing any specimen.
                if (!currentSample(context, sampler, before, area) || !center.current(level)) return InteractionResult.FAIL;
                // A later permission can change an earlier plot; both reports and research
                // must use the original soil, crop entity and saved crop data.
                java.util.function.Predicate<BlockPos> available = plot -> allowed.containsKey(plot)
                        && allowed.get(plot).current(level);
                SoilSurveyData.save(sampler, SoilSurveyData.survey(level, soil, area, available));
                SoilResearch.gather(sampler, level, soil, area, available);
            }
            if (area) {
                player.displayClientMessage(Component.literal(farmland
                        ? "5x5 soil survey recorded. Use normally to collect one physical soil specimen."
                        : "Use normally on dirt to collect a soil specimen."), true);
            } else if (SoilResearch.collect(sampler, level, soil)) {
                ItemStack specimen = SoilSpecimenItem.from(level, soil);
                player.getInventory().placeItemBackInInventory(specimen);
                player.displayClientMessage(Component.literal("Soil specimen collected! Analyze it at a Soil Research Table."), true);
            } else {
                player.displayClientMessage(Component.literal("Already collected this plot (or sampler full). Find fresh soil."), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean maySamplePlot(UseOnContext context, Plot plot, ItemStack sampler, ItemStack before, boolean area) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (!WorkstationAccess.mayUse(level, plot.soil(), player, context.getClickedFace(), sampler)
                || !currentSample(context, sampler, before, area) || !plot.current(level)) return false;
        return (!(plot.plantState().getBlock() instanceof AgriCropBlock)
                    || WorkstationAccess.mayUse(level, plot.soil().above(), player, context.getClickedFace(), sampler))
                && currentSample(context, sampler, before, area) && plot.current(level);
    }

    private static boolean currentSample(UseOnContext context, ItemStack sampler, ItemStack before, boolean area) {
        var player = context.getPlayer();
        return WorkstationAccess.mayCompleteItemUse(context.getLevel(), player, context.getHand(), sampler, before)
                && player.isShiftKeyDown() == area;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) SoilSamplerClientScreens.open(held);
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        int tier = SoilResearch.researchedTier(stack);
        lines.add(Component.literal("Research tier: " + tier + "/5").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Specimens analyzed at table: " + SoilResearch.analyzed(stack) + "/96"));
        lines.add(Component.literal("Right-click dirt/farmland: collect physical soil specimen"));
        lines.add(Component.literal("Sneak + right-click farmland/crop: 5x5 survey"));
        lines.add(Component.literal("Insert sampler + specimen in Soil Research Table GUI"));
        lines.add(Component.literal("Right-click air: open soil report"));

        if (tier < 5) lines.add(Component.literal("Next table unlock: " + SoilResearch.nextUnlock(tier) + " analyzed samples"));
        if (SoilSurveyData.hasReport(stack)) {
            CompoundTag sample = SoilSurveyData.report(stack);
            lines.add(Component.literal("Last reading: " + sample.getInt("Valid") + " plots"));
        }
    }
}

