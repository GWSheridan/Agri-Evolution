package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.SeedQuality;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

/** Each seed item plants only its matching registered species; the genome belongs to the stack. */
public class SpeciesSeedItem extends Item {
    private final CropSpecies species;
    private final Supplier<? extends AgriCropBlock> cropBlock;

    public SpeciesSeedItem(Properties properties, CropSpecies species, Supplier<? extends AgriCropBlock> cropBlock) {
        super(properties);
        this.species = species;
        this.cropBlock = cropBlock;
    }

    /** The analyzer uses the registered seed type; never guesses species from texture or tooltip. */
    public CropSpecies species() { return species; }
    public AgriCropBlock cropBlock() { return cropBlock.get(); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos soilPos = context.getClickedPos();
        BlockPos cropPos = soilPos.above();
        if (context.getClickedFace() != net.minecraft.core.Direction.UP
                || !com.agrievolution.farming.SoilResearch.isSoil(level.getBlockState(soilPos))
                || !level.getBlockState(cropPos).isAir()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            CropGenome genome = GenomeStorage.fromSeed(context.getItemInHand());
            if (level.setBlock(cropPos, cropBlock.get().defaultBlockState(), 3)) {
                if (level.getBlockEntity(cropPos) instanceof AECropBlockEntity crop) {
                    crop.setGenome(genome);
                    crop.setRareTrait(RareTraitStorage.fromSeed(context.getItemInHand()));
                    crop.setLineage(HybridLineageStorage.fromSeed(context.getItemInHand()));
                    crop.setVariety(com.agrievolution.genetics.SeedVariety.name(context.getItemInHand()));
                }
                if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        CropGenome genome = GenomeStorage.fromSeed(stack);
        lines.add(Component.literal(species.displayName() + (species.isMutation() ? "  |  Mutation" : "")));
        lines.add(Component.literal("Quality " + SeedQuality.rating(genome)
                + " (" + SeedQuality.score(genome) + "/100)"));
        lines.add(Component.literal("Growth " + genome.growth()
                + "  |  Yield " + genome.yield()
                + "  |  Resilience " + genome.resilience()));
        if (species.isResource()) {
            lines.add(Component.literal("Resource crop - breed for higher tiers")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            lines.add(Component.literal(com.agrievolution.farming.ResourceCropRules.catalystHint(species))
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
            if (species == CropSpecies.NETHERITE) {
                lines.add(Component.literal("Harvest: Netherite Scrap (combine with Gold)")
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
            }
        }
        HybridLineageStorage.Lineage lineage = HybridLineageStorage.fromSeed(stack);
        if (lineage != null) {
            lines.add(Component.literal("HYBRID OFFSPRING").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            lines.add(Component.literal("Parents: " + lineage.description())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(Component.literal("Result: " + species.displayName())
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        }
        RareTrait trait = RareTraitStorage.fromSeed(stack);
        if (trait != RareTrait.NONE)
            lines.add(Component.literal("Rare Trait: " + trait.description()));
    }
}
