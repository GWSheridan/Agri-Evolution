package com.agrievolution.content;

import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.RareTraitStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** One non-destructive, genotype-accurate genetic sample from a mature planted crop. */
public final class GeneticClippersItem extends Item {
    public GeneticClippersItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Use on a fully grown Agri Evolution crop."));
        tooltip.add(Component.literal("Takes one genetic sample without harvesting."));
        tooltip.add(Component.literal("One sample per planted crop; replant to sample again."));
        tooltip.add(Component.literal("Use DNA samples in the Botanical Genetics Laboratory."));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AgriCropBlock crop)) return InteractionResult.PASS;
        if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (state.getValue(CropBlock.AGE) < crop.getMaxAge()) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal("Wait for the crop to mature before clipping."), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(level.getBlockEntity(pos) instanceof AECropBlockEntity plant)) return InteractionResult.PASS;
        if (plant.hasBeenClipped()) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal("Already clipped. Replant for another sample."), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack sourceSeed = new ItemStack(AEItems.seedFor(crop.species()));
            GenomeStorage.putOnSeed(sourceSeed, plant.genome());
            RareTraitStorage.putOnSeed(sourceSeed, plant.rareTrait());
            if (plant.lineage() != null) HybridLineageStorage.putOnSeed(sourceSeed, plant.lineage());
            com.agrievolution.genetics.SeedVariety.apply(sourceSeed, plant.variety());
            ItemStack geneticSample = GeneticSampleItem.capture(sourceSeed);
            plant.setClipped(true); // Persist before giving the sample, so re-use cannot duplicate it.
            player.getInventory().placeItemBackInInventory(geneticSample);
            player.displayClientMessage(Component.literal("Genetic clipping collected. The crop remains planted."), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
