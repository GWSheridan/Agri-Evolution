package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Workstation jobs consume exact items from the player's main inventory in one transaction. */
public final class GardenWork {
    private static final String ORDERS_KEY = "AgriGardenOrders";
    private static final String COMMISSIONS_KEY = "AgriGardenCommissions";
    private static final String SEED_COMMISSIONS_KEY = "AgriSeedCommissions";
    public static final int ROWS = 3, UNLOCK_AT = 2;

    public record Ingredient(Supplier<? extends Item> item, int count, int growth, int yield, int resilience, boolean rare) {
        public Ingredient(Supplier<? extends Item> item, int count) { this(item, count, 0, 0, 0, false); }
        public ItemStack stack() { return new ItemStack(item.get(), count); }

        private boolean matches(ItemStack stack, Item required) {
            if (!stack.is(required)) return false;
            if (!qualified()) return true;
            if (!(stack.getItem() instanceof SpeciesSeedItem)) return false;
            var genes = GenomeStorage.fromSeed(stack);
            return genes.growth() >= growth && genes.yield() >= yield && genes.resilience() >= resilience
                    && (!rare || RareTraitStorage.fromSeed(stack) != RareTrait.NONE);
        }

        public boolean qualified() { return growth != 0 || yield != 0 || resilience != 0 || rare; }

        public String requirements(boolean compact) {
            var rules = new java.util.ArrayList<String>();
            if (growth > 0) rules.add("Growth ≥ " + growth);
            if (yield > 0) rules.add("Yield ≥ " + yield);
            if (resilience > 0) rules.add((compact ? "Res. ≥ " : "Resilience ≥ ") + resilience);
            if (rare) rules.add("Any rare trait");
            return String.join(compact ? " " : ", ", rules);
        }
    }

    public record Job(String title, List<Ingredient> ingredients, Ingredient reward) {
        public boolean ready(Inventory inventory) {
            return plan(inventory) != null;
        }

        private int[] plan(Inventory inventory) {
            var consumed = new int[inventory.items.size()];
            for (var ingredient : ingredients) {
                int remaining = ingredient.count();
                Item item = ingredient.item().get();
                for (int slot = 0; slot < consumed.length && remaining > 0; slot++) {
                    var stack = inventory.items.get(slot);
                    if (consumed[slot] >= stack.getCount() || !ingredient.matches(stack, item)) continue;
                    int used = Math.min(remaining, stack.getCount() - consumed[slot]);
                    consumed[slot] += used;
                    remaining -= used;
                }
                if (remaining > 0) return null;
            }
            return consumed;
        }

        boolean complete(Player player) {
            var inventory = player.getInventory();
            var consumed = plan(inventory);
            if (consumed == null) return false;
            for (int slot = 0; slot < consumed.length; slot++) {
                if (consumed[slot] == 0) continue;
                var stack = inventory.items.get(slot);
                stack.shrink(consumed[slot]);
                if (stack.isEmpty()) inventory.items.set(slot, ItemStack.EMPTY);
            }
            var result = reward.stack();
            inventory.add(result);
            if (!result.isEmpty()) player.drop(result, false);
            inventory.setChanged();
            return true;
        }
    }

    private static final List<List<Job>> ORDERS = List.of(
            List.of(order("Salad Harvest", 1, use(AEItems.TOMATO, 6), use(AEItems.LETTUCE, 6), use(AEItems.CUCUMBER, 4)),
                    order("Market Vegetables", 1, use(AEItems.RADISH, 6), use(AEItems.CORN, 6), use(AEItems.CABBAGE, 4)),
                    order("Roots and Herbs", 1, use(AEItems.ONION, 6), use(() -> Items.CARROT, 6), use(AEItems.BASIL, 4))),
            List.of(order("Lunch Service", 2, dish("garden_sandwich", 3), dish("tomato_basil_soup", 3)),
                    order("Kitchen Counter", 2, dish("leek_potato_soup", 3), dish("salmon_dill_sandwich", 3)),
                    order("Pantry Restock", 2, use(AEItems.BERRY_JAM, 2), use(AEItems.DILL_PICKLES, 2), use(AEItems.GARDEN_SEASONING, 2))),
            List.of(order("Harvest Feast", 4, dish("roast_chicken_dinner", 3), dish("strawberry_tart", 3),
                            use(AEItems.BERRY_JAM, 2), use(AEItems.CORN, 8)),
                    order("Market Supper", 4, dish("pork_harvest_plate", 3), dish("chickpea_salad_wrap", 3),
                            use(AEItems.DILL_PICKLES, 2), use(AEItems.GARDEN_SEASONING, 2)),
                    order("Sunday Dinner", 4, dish("hearty_beef_stew", 3), dish("blackberry_crumble", 3),
                            dish("roasted_vegetable_bowl", 3)))
    );

    private static final List<Job> COMMISSIONS = List.of(
            new Job("Market Garden", List.of(use(AEItems.TOMATO, 8), use(AEItems.LETTUCE, 8),
                    use(AEItems.RADISH, 8), use(AEItems.CABBAGE, 8)), use(AEItems.NUTRIENT_COMPOST, 12)),
            new Job("Pantry Collection", List.of(use(AEItems.BERRY_JAM, 4), use(AEItems.DILL_PICKLES, 4),
                    use(AEItems.GARDEN_SEASONING, 4)), use(AEItems.IRON_CROP_SUPPORT, 8)),
            new Job("A Full Table", List.of(dish("garden_sandwich", 4), dish("leek_potato_soup", 4),
                    dish("roast_chicken_dinner", 4), dish("strawberry_tart", 4)), use(AEItems.OBSIDIAN_CROP_SUPPORT, 4))
    );
    private static final List<Job> SEED_COMMISSIONS = List.of(
            new Job("Kitchen Garden Seed Trial", List.of(new Ingredient(AEItems.TOMATO_SEED, 4, 0, 70, 0, false)),
                    use(AEItems.NUTRIENT_COMPOST, 12)),
            new Job("Fast and Hardy", List.of(new Ingredient(AEItems.CORN_SEED, 3, 65, 0, 65, false)),
                    use(AEItems.IRON_CROP_SUPPORT, 8)),
            new Job("Rare Lettuce", List.of(new Ingredient(AEItems.LETTUCE_SEED, 2, 0, 0, 0, true)),
                    use(AEItems.OBSIDIAN_CROP_SUPPORT, 4))
    );

    private static final List<Job> PRESERVES = List.of(
            new Job("Berry Jam", List.of(use(AEItems.STRAWBERRY, 3), use(AEItems.BLUEBERRY, 3),
                    use(() -> Items.SUGAR, 1), use(() -> Items.GLASS_BOTTLE, 1)), use(AEItems.BERRY_JAM, 1)),
            new Job("Dill Pickles", List.of(use(AEItems.CUCUMBER, 4), use(AEItems.DILL, 2),
                    use(() -> Items.GLASS_BOTTLE, 1)), use(AEItems.DILL_PICKLES, 1)),
            new Job("Garden Seasoning", List.of(use(AEItems.THYME, 3), use(AEItems.ROSEMARY, 3),
                    use(() -> Items.GLASS_BOTTLE, 1)), use(AEItems.GARDEN_SEASONING, 1))
    );

    public static List<Job> preservingRecipes() { return PRESERVES; }

    static Ingredient use(Supplier<? extends Item> item, int count) { return new Ingredient(item, count); }

    private static Ingredient dish(String id, int count) {
        return use(() -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(AgriEvolution.MOD_ID, id)), count);
    }

    private static Job order(String title, int emeralds, Ingredient... ingredients) {
        return new Job(title, List.of(ingredients), use(() -> Items.EMERALD, emeralds));
    }

    static Job job(boolean preserving, int page, int row) {
        if (preserving) return PRESERVES.get(row);
        if (page == ROWS) return COMMISSIONS.get(row);
        if (page == ROWS + 1) return SEED_COMMISSIONS.get(row);
        return ORDERS.get(page).get(row);
    }

    static int completed(Player player, int tier) {
        var counts = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getIntArray(ORDERS_KEY);
        return tier < counts.length ? Math.clamp(counts[tier], 0, Short.MAX_VALUE - 1) : 0;
    }

    static void recordOrder(Player player, int tier) {
        var counts = new int[ROWS];
        for (int row = 0; row < ROWS; row++) counts[row] = completed(player, row);
        counts[tier] = Math.min(Short.MAX_VALUE - 1, counts[tier] + 1);
        var root = player.getPersistentData();
        var persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putIntArray(ORDERS_KEY, counts);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    static void copyOrders(net.minecraft.nbt.CompoundTag original, net.minecraft.nbt.CompoundTag next) {
        if (original.contains(ORDERS_KEY)) next.putIntArray(ORDERS_KEY, original.getIntArray(ORDERS_KEY));
        if (original.contains(COMMISSIONS_KEY)) next.putInt(COMMISSIONS_KEY, original.getInt(COMMISSIONS_KEY));
        if (original.contains(SEED_COMMISSIONS_KEY)) next.putInt(SEED_COMMISSIONS_KEY, original.getInt(SEED_COMMISSIONS_KEY));
    }

    static int commissions(Player player) {
        return commissions(player, false);
    }

    static int commissions(Player player, boolean seeds) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)
                .getInt(seeds ? SEED_COMMISSIONS_KEY : COMMISSIONS_KEY) & 7;
    }

    static void recordCommission(Player player, int row, boolean seeds) {
        var root = player.getPersistentData();
        var persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(seeds ? SEED_COMMISSIONS_KEY : COMMISSIONS_KEY, commissions(player, seeds) | (1 << row));
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private GardenWork() {}
}
