package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import java.util.List;

/** A consumable specimen containing a snapshot of one seed's traits. */
public final class GeneticSampleItem extends Item {
    private static final String KEY = "AgriEvolutionGeneticSample";
    public GeneticSampleItem(Properties properties) { super(properties); }

    public static ItemStack capture(ItemStack seed) {
        ItemStack sample = new ItemStack(AEItems.GENETIC_SAMPLE.get());
        CompoundTag data = new CompoundTag();
        data.put("Genome", GenomeStorage.writeGenome(GenomeStorage.fromSeed(seed)));
        data.putString("RareTrait", RareTraitStorage.fromSeed(seed).name());
        if (seed.getItem() instanceof SpeciesSeedItem type) data.putString("Species", type.species().name());
        CompoundTag custom = new CompoundTag();
        custom.put(KEY, data);
        sample.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        return sample;
    }

    private static CompoundTag sampleData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(KEY);
    }
    public static boolean isValid(ItemStack stack) {
        return stack.is(AEItems.GENETIC_SAMPLE.get()) && sampleData(stack).contains("Genome", 10);
    }
    public static CropGenome genome(ItemStack stack) {
        return GenomeStorage.readGenome(sampleData(stack).getCompound("Genome"));
    }
    public static RareTrait rareTrait(ItemStack sample) {
        return RareTrait.fromName(sampleData(sample).getString("RareTrait"));
    }
    public static String speciesName(ItemStack stack) {
        String key = sampleData(stack).getString("Species");
        var species = com.agrievolution.genetics.RetiredResourceCrops.species(key.toLowerCase(java.util.Locale.ROOT));
        return species == null ? "Unknown specimen" : species.displayName();
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
                                          List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!isValid(stack)) { tooltip.add(Component.literal("Empty genetic specimen")); return; }
        CropGenome genome = genome(stack);
        tooltip.add(Component.literal("Source: " + speciesName(stack)));
        tooltip.add(Component.literal("Growth " + genome.growth() + "  Yield " + genome.yield()
                + "  Resilience " + genome.resilience()));
        if (rareTrait(stack) != RareTrait.NONE)
            tooltip.add(Component.literal("Archived: " + rareTrait(stack).description()));
        tooltip.add(Component.literal("Consumed when grafting or evolving another seed"));
    }
}
