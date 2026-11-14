package com.agrievolution.content;

import com.agrievolution.client.JournalClientScreens;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeBreeder;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.SeedVariety;
import com.agrievolution.genetics.SpeciesBreeder;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Portable discovery record and two-parent genetics tool. Discoveries are saved
 * in this particular journal's CUSTOM_DATA and travel with the item on servers.
 */
public final class BreedingJournalItem extends Item {
    private static final String SELECTED = "AgriSelectedParent";
    private static final String DISCOVERED = "AgriDiscoveredSpecies";
    private static final String RECORDS = "AgriSpeciesRecords";
    private static final String RARE_DISCOVERIES = "AgriRareGenes";
    private static final String POS = "Pos";
    private static final String DIM = "Dimension";

    public BreedingJournalItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (!(state.getBlock() instanceof AgriCropBlock crop) || player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof AECropBlockEntity target)) {
            player.displayClientMessage(Component.literal("Crop genetics unavailable; replant this crop."), true);
            return InteractionResult.CONSUME;
        }

        ItemStack journal = context.getItemInHand();
        CompoundTag root = journal.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        com.agrievolution.content.ResourceCropMigration.journal(root);
        CropGenome genome = target.genome();
        boolean mature = state.getValue(AgriCropBlock.AGE) == 7;

        discover(root, crop.species());
        recordBestGenetics(root, crop.species(), genome);
        SeedVariety.record(root, target.variety(), crop.species(), genome, target.lineage());
        boolean newTrait = recordTrait(root, target.rareTrait());

        if (player.isShiftKeyDown()) {
            if (!mature) {
                save(journal, root);
                player.displayClientMessage(Component.literal("Select a fully grown crop (stage 7)."), true);
                return InteractionResult.CONSUME;
            }
            if (target.hasCrossed()) {
                save(journal, root);
                player.displayClientMessage(Component.literal("This crop has already been crossed."), true);
                return InteractionResult.CONSUME;
            }
            CompoundTag selected = new CompoundTag();
            selected.putLong(POS, pos.asLong());
            selected.putString(DIM, level.dimension().location().toString());
            root.put(SELECTED, selected);
            save(journal, root);
            status(player, "Parent A selected: " + crop.species().displayName());
            return InteractionResult.CONSUME;
        }

        if (!root.contains(SELECTED, Tag.TAG_COMPOUND)) {
            save(journal, root);
            status(player, newTrait ? "NEW GENETIC DISCOVERY: " + target.rareTrait().name()
                    : crop.species().displayName() + " recorded - open journal for details");
            return InteractionResult.CONSUME;
        }

        CompoundTag selected = root.getCompound(SELECTED);
        BlockPos parentPos = BlockPos.of(selected.getLong(POS));
        if (!selected.getString(DIM).equals(level.dimension().location().toString())
                || parentPos.equals(pos) || parentPos.distSqr(pos) > 64.0D
                || !level.hasChunkAt(parentPos)) {
            save(journal, root);
            status(player, "Use a different mature crop within 8 blocks.");
            return InteractionResult.CONSUME;
        }
        BlockState parentState = level.getBlockState(parentPos);
        if (!(parentState.getBlock() instanceof AgriCropBlock first)
                || parentState.getValue(AgriCropBlock.AGE) != 7 || !mature
                || !(level.getBlockEntity(parentPos) instanceof AECropBlockEntity parent)) {
            save(journal, root);
            status(player, "Both parent crops must still be fully grown.");
            return InteractionResult.CONSUME;
        }
        if (parent.hasCrossed() || target.hasCrossed()) {
            save(journal, root);
            status(player, "A parent has already been bred; replant it.");
            return InteractionResult.CONSUME;
        }

        // Visible supports on BOTH parents add five percentage points to rare mutations.
        int bonus = CropSupportItem.fieldBonus(Math.min(CropSupportItem.tier(parentState),CropSupportItem.tier(state)));
        int pollination = parent.pollinated(level.getGameTime()) || target.pollinated(level.getGameTime()) ? CropPollination.BONUS : 0;
        CropSpecies offspringSpecies = SpeciesBreeder.cross(first.species(), crop.species(),
                level.random.nextLong(), bonus, pollination);
        CropGenome offspringGenome = GenomeBreeder.cross(parent.genome(), genome, level.random.nextLong());
        ItemStack seed = new ItemStack(AEItems.seedFor(offspringSpecies));
        if (first.species() != crop.species()) HybridLineageStorage.putOnSeed(seed,
                new HybridLineageStorage.Lineage(first.species(), crop.species()));
        else if (offspringSpecies == crop.species() && parent.variety().equals(target.variety())) {
            SeedVariety.apply(seed, target.variety());
            if (target.lineage() != null && target.lineage().equals(parent.lineage())) {
                HybridLineageStorage.putOnSeed(seed, target.lineage());
            }
        }
        GenomeStorage.putOnSeed(seed, offspringGenome);
        RareTrait inherited = RareTrait.inherit(parent.rareTrait(), target.rareTrait(),
                new java.util.Random(level.random.nextLong()));
        RareTraitStorage.putOnSeed(seed, inherited);
        recordTrait(root, inherited);
        SeedVariety.record(root, SeedVariety.name(seed), offspringSpecies, offspringGenome, HybridLineageStorage.fromSeed(seed));
        if (!player.getInventory().add(seed)) player.drop(seed, false);
        parent.setCrossed(true);
        target.setCrossed(true);
        root.remove(SELECTED);
        boolean newSpecies = discover(root, offspringSpecies);
        recordBestGenetics(root, offspringSpecies, offspringGenome);
        save(journal, root);
        FarmingMilestones.CROSS.award(player);

        status(player, "Crossed: " + offspringSpecies.displayName()
                + (newSpecies ? " - NEW DISCOVERY!" : " - seed produced!")
                + (pollination > 0 ? " (bee-pollinated)" : ""));
        return InteractionResult.CONSUME;
    }

    /** Read the saved collection in the real vanilla paginated book GUI. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            JournalClientScreens.open(stack);
        } else if (player instanceof net.minecraft.server.level.ServerPlayer server) FarmingMilestones.sync(server);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Keep the strongest observed value of each trait on this journal item. */
    private static void recordBestGenetics(CompoundTag data, CropSpecies species, CropGenome genes) {
        CompoundTag all = data.getCompound(RECORDS);
        CompoundTag record = all.getCompound(species.id());
        record.putInt("Growth", Math.max(record.getInt("Growth"), genes.growth()));
        record.putInt("Yield", Math.max(record.getInt("Yield"), genes.yield()));
        record.putInt("Resilience", Math.max(record.getInt("Resilience"), genes.resilience()));
        all.put(species.id(), record);
        data.put(RECORDS, all);
    }

    /** Store new research on this particular journal; older journals remain readable. */
    private static boolean recordTrait(CompoundTag root, RareTrait trait) {
        if (trait == RareTrait.NONE) return false;
        ListTag found = root.getList(RARE_DISCOVERIES, Tag.TAG_STRING);
        for (int i = 0; i < found.size(); i++)
            if (found.getString(i).equals(trait.name())) return false;
        found.add(StringTag.valueOf(trait.name()));
        root.put(RARE_DISCOVERIES, found);
        return true;
    }
    private static void status(Player player, String message) {
        // Action bar instead of filling normal chat with discovery and breeding details.
        player.displayClientMessage(Component.literal(message), true);
    }

    private static void save(ItemStack journal, CompoundTag data) {
        journal.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    private static ListTag discoveries(CompoundTag root) {
        return root.getList(DISCOVERED, Tag.TAG_STRING);
    }

    private static boolean containsSpecies(ListTag found, String id) {
        for (int i = 0; i < found.size(); i++) {
            if (found.getString(i).equals(id)) return true;
        }
        return false;
    }

    private static boolean discover(CompoundTag root, CropSpecies species) {
        ListTag found = discoveries(root);
        if (containsSpecies(found, species.id())) return false;
        found.add(StringTag.valueOf(species.id()));
        root.put(DISCOVERED, found);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        com.agrievolution.content.ResourceCropMigration.journal(root);
        lines.add(Component.literal("Species discovered: " + discoveries(root).size()
                + "/" + CropSpecies.values().length));
        lines.add(Component.literal("Right-click air: open the journal book"));
        lines.add(Component.literal("Right-click crop: inspect / record species"));
        lines.add(Component.literal("Sneak + right-click mature crop: select parent A"));
        lines.add(Component.literal("Right-click another mature crop: crossbreed"));
        if (root.contains(SELECTED, Tag.TAG_COMPOUND)) {
            lines.add(Component.literal("Parent A selected"));
        }
    }
}
