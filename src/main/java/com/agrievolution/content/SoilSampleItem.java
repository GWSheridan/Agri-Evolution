package com.agrievolution.content;

import com.agrievolution.client.SoilSamplerClientScreens;
import com.agrievolution.farming.SoilResearch;
import com.agrievolution.farming.SoilSurveyData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Soil research is bound to this sampler stack and unlocks five farmland tiers. */
public final class SoilSampleItem extends Item {
    public SoilSampleItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);
        boolean crop = clickedState.getBlock() instanceof AgriCropBlock;
        BlockPos soil = crop ? clicked.below() : clicked;
        BlockState state = level.getBlockState(soil);
        boolean farmland = SoilResearch.isSoil(state);
        boolean naturalDirt = state.is(net.minecraft.world.level.block.Blocks.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.PODZOL);
        if (!farmland && !naturalDirt) return InteractionResult.PASS;
        ItemStack sampler = context.getItemInHand();
        ItemStack before = sampler.copy();
        if (sampler.getItem() != this || !maySamplePlot(player, level, soil, context.getClickedFace(), sampler)
                || !currentSample(context, sampler, before, clickedState, soil, state)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            boolean area = player.isShiftKeyDown();
            if (farmland) {
                var allowed = new java.util.HashMap<BlockPos, BlockState>();
                allowed.put(soil, state);
                if (area) for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
                    BlockPos plot = soil.offset(dx, 0, dz);
                    if (plot.equals(soil) || !level.hasChunkAt(plot)) continue;
                    BlockState plotState = level.getBlockState(plot), plantState = level.getBlockState(plot.above());
                    if (!SoilResearch.isSoil(plotState)) continue;
                    if (maySamplePlot(player, level, plot, context.getClickedFace(), sampler)
                            && level.getBlockState(plot) == plotState && level.getBlockState(plot.above()) == plantState) {
                        allowed.put(plot, plotState);
                    }
                    if (!currentSample(context, sampler, before, clickedState, soil, state)) return InteractionResult.FAIL;
                }
                // Gather permission decisions before changing the held tool or issuing any specimen.
                if (!currentSample(context, sampler, before, clickedState, soil, state)) return InteractionResult.FAIL;
                java.util.function.Predicate<BlockPos> available = plot -> allowed.containsKey(plot)
                        && level.getBlockState(plot) == allowed.get(plot);
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

    private static boolean maySamplePlot(Player player, Level level, BlockPos soil, Direction face, ItemStack sampler) {
        if (!WorkstationAccess.mayUse(level, soil, player, face, sampler)) return false;
        BlockPos plant = soil.above();
        return !(level.getBlockState(plant).getBlock() instanceof AgriCropBlock)
                || WorkstationAccess.mayUse(level, plant, player, face, sampler);
    }

    private static boolean currentSample(UseOnContext context, ItemStack sampler, ItemStack before,
                                         BlockState clickedState, BlockPos soil, BlockState soilState) {
        var player = context.getPlayer();
        var level = context.getLevel();
        return player != null && player.level() == level && player.isAlive() && !player.isSpectator() && player.mayBuild()
                && player.getItemInHand(context.getHand()) == sampler && ItemStack.matches(before, sampler)
                && level.getBlockState(context.getClickedPos()) == clickedState && level.getBlockState(soil) == soilState;
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

