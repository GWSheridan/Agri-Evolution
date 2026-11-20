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
        BlockPos soilPos = context.getClickedPos().immutable();
        BlockPos cropPos = soilPos.above();
        if (context.getClickedFace() != net.minecraft.core.Direction.UP
                || level.isOutsideBuildHeight(soilPos) || level.isOutsideBuildHeight(cropPos)
                || !level.hasChunkAt(soilPos)) return InteractionResult.PASS;
        BlockState soil = level.getBlockState(soilPos);
        BlockState target = level.getBlockState(cropPos);
        if (!com.agrievolution.farming.SoilResearch.isSoil(soil) || !target.isAir()) {
            return InteractionResult.PASS;
        }
        var player = context.getPlayer();
        var held = context.getItemInHand();
        var before = held.copy();
        boolean mayBuild = player != null && player.mayBuild();
        if (!currentSeed(context, held, before, mayBuild)) return InteractionResult.FAIL;
        var soilEntity = level.getBlockEntity(soilPos);
        if (player != null && (player.isSpectator()
                || !level.mayInteract(player, soilPos) || !level.mayInteract(player, cropPos)
                || !player.mayUseItemAt(cropPos, context.getClickedFace(), held))) {
            return InteractionResult.FAIL;
        }
        if (!currentSeed(context, held, before, mayBuild)
                || level.getBlockState(soilPos) != soil || level.getBlockState(cropPos) != target
                || level.getBlockEntity(soilPos) != soilEntity
                || soilEntity != null && (soilEntity.getLevel() != level || !WorkstationAccess.isCurrent(soilEntity))) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            CropGenome genome = GenomeStorage.fromSeed(before);
            BlockState plantedState = cropBlock.get().defaultBlockState();
            boolean spendSeed = player == null || !player.getAbilities().instabuild;
            if (level.setBlock(cropPos, plantedState, 3)) {
                if (level.getBlockState(cropPos) == plantedState && level.getBlockEntity(cropPos) instanceof AECropBlockEntity crop
                        && crop.getLevel() == level && WorkstationAccess.isCurrent(crop)) {
                    crop.setGenome(genome);
                    crop.setRareTrait(RareTraitStorage.fromSeed(before));
                    crop.setLineage(HybridLineageStorage.fromSeed(before));
                    crop.setVariety(com.agrievolution.genetics.SeedVariety.name(before));
                }
                if (spendSeed) held.shrink(1);
            } else return InteractionResult.FAIL;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean currentSeed(UseOnContext context, ItemStack held, ItemStack before, boolean mayBuild) {
        var player = context.getPlayer();
        // Keep playerless automation and vanilla Adventure CanPlaceOn permissions.
        return !held.isEmpty() && held.getItem() == this && ItemStack.matches(before, held)
                && (player == null || player.isAlive() && player.level() == context.getLevel() && !player.isSpectator()
                    && player.mayBuild() == mayBuild && player.getItemInHand(context.getHand()) == held);
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
