package com.agrievolution.content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

final class SharedMarketOrders extends SavedData {
    private static final SavedData.Factory<SharedMarketOrders> FACTORY =
            new SavedData.Factory<>(SharedMarketOrders::new, SharedMarketOrders::load, null);
    static final List<GardenWork.Job> ORDERS = List.of(
            order("Village Produce Stand", GardenWork.use(AEItems.TOMATO, 32), GardenWork.use(AEItems.LETTUCE, 24),
                    GardenWork.use(AEItems.CUCUMBER, 24)),
            order("Market Lunch", GardenWork.dish("garden_sandwich", 16), GardenWork.dish("tomato_basil_soup", 16)),
            order("Winter Pantry", GardenWork.use(AEItems.BERRY_JAM, 24), GardenWork.use(AEItems.DILL_PICKLES, 24),
                    GardenWork.use(AEItems.GARDEN_SEASONING, 16))
    );

    private final List<Progress> progress = List.of(new Progress(0), new Progress(1), new Progress(2));

    private static GardenWork.Job order(String title, GardenWork.Ingredient... ingredients) {
        return new GardenWork.Job(title, List.of(ingredients), GardenWork.use(() -> Items.EMERALD, 1));
    }

    private static final class Progress {
        private final int[] delivered;
        private final Map<UUID, Integer> contributors = new HashMap<>();
        private final Map<UUID, Integer> rewards = new HashMap<>();

        private Progress(int row) { delivered = new int[ORDERS.get(row).ingredients().size()]; }
    }

    static SharedMarketOrders get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "agri_shared_market_orders");
    }

    int remaining(int row, int ingredient) {
        var ingredients = ORDERS.get(row).ingredients();
        return ingredient >= ingredients.size() ? 0 : ingredients.get(ingredient).count() - progress.get(row).delivered[ingredient];
    }

    int reward(int row, UUID player) { return progress.get(row).rewards.getOrDefault(player, 0); }

    boolean contribute(int row, Player player) {
        var order = ORDERS.get(row);
        var round = progress.get(row);
        int added = 0;
        for (int i = 0; i < order.ingredients().size(); i++) {
            int needed = remaining(row, i);
            var item = order.ingredients().get(i).item().get();
            for (ItemStack stack : player.getInventory().items) {
                if (needed == 0) break;
                if (!stack.is(item)) continue;
                int offered = Math.min(stack.getCount(), needed);
                stack.shrink(offered);
                needed -= offered;
                round.delivered[i] += offered;
                added += offered;
            }
        }
        if (added == 0) return false;
        round.contributors.merge(player.getUUID(), added, Integer::sum);
        boolean complete = true;
        for (int i = 0; i < round.delivered.length; i++) complete &= remaining(row, i) == 0;
        if (complete) {
            round.contributors.forEach((id, amount) -> round.rewards.merge(id, 1 + amount / 16, (a, b) -> Math.min(32767, a + b)));
            round.contributors.clear();
            java.util.Arrays.fill(round.delivered, 0);
        }
        setDirty();
        player.getInventory().setChanged();
        return true;
    }

    boolean claim(int row, Player player) {
        var rewards = progress.get(row).rewards;
        int amount = Math.min(64, reward(row, player.getUUID()));
        if (amount == 0) return false;
        var stack = new ItemStack(Items.EMERALD, amount);
        player.getInventory().add(stack);
        int claimed = amount - stack.getCount();
        if (claimed == 0) return false;
        int left = rewards.get(player.getUUID()) - claimed;
        if (left == 0) rewards.remove(player.getUUID());
        else rewards.put(player.getUUID(), left);
        setDirty();
        return true;
    }

    @Override public CompoundTag save(CompoundTag data, HolderLookup.Provider registries) {
        for (int row = 0; row < progress.size(); row++) {
            var round = progress.get(row);
            var record = new CompoundTag();
            record.putIntArray("Delivered", round.delivered);
            record.put("Contributors", counts(round.contributors));
            record.put("Rewards", counts(round.rewards));
            data.put("Order" + row, record);
        }
        return data;
    }

    private static SharedMarketOrders load(CompoundTag data, HolderLookup.Provider registries) {
        var orders = new SharedMarketOrders();
        for (int row = 0; row < orders.progress.size(); row++) {
            var record = data.getCompound("Order" + row);
            var round = orders.progress.get(row);
            var delivered = record.getIntArray("Delivered");
            for (int i = 0; i < Math.min(delivered.length, round.delivered.length); i++) {
                round.delivered[i] = Math.clamp(delivered[i], 0, ORDERS.get(row).ingredients().get(i).count());
            }
            readCounts(record.getList("Contributors", Tag.TAG_COMPOUND), round.contributors);
            readCounts(record.getList("Rewards", Tag.TAG_COMPOUND), round.rewards);
        }
        return orders;
    }

    private static ListTag counts(Map<UUID, Integer> counts) {
        var result = new ListTag();
        counts.forEach((id, count) -> {
            var member = new CompoundTag();
            member.putUUID("Player", id);
            member.putInt("Count", count);
            result.add(member);
        });
        return result;
    }

    private static void readCounts(ListTag tags, Map<UUID, Integer> result) {
        for (Tag tag : tags) {
            var member = (CompoundTag) tag;
            int count = Math.clamp(member.getInt("Count"), 0, 32767);
            if (count > 0 && member.hasUUID("Player")) result.put(member.getUUID("Player"), count);
        }
    }
}
