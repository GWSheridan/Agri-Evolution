package com.agrievolution.content;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GardenWorkMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final boolean serverPreserving;
    private final List<GardenWork.Job> customOrders;
    private int page;
    public static final int SEED_PAGE = GardenWork.ROWS + 1, SHARED_PAGE = SEED_PAGE + 1, CUSTOM_PAGE = SHARED_PAGE + 1;
    private static final int PAGE_BUTTON = 64;
    private static final int SHARED_CLAIM = 48;
    private static final int SHARED_START = 7, SHARED_FIELDS = 8;
    private static final int REWARD = 4, CONTRIBUTED = 5, ROUND_LOW = 6, ROUND_HIGH = 7;
    private static final int DATA_COUNT = SHARED_START + GardenWork.ROWS * SHARED_FIELDS;

    public GardenWorkMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL, false, List.of(), new SimpleContainerData(DATA_COUNT));
    }

    GardenWorkMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, ContainerLevelAccess.NULL, false, GardenOrderReload.read(buffer), new SimpleContainerData(DATA_COUNT));
    }

    GardenWorkMenu(int id, Inventory inventory, ContainerLevelAccess access, boolean preserving) {
        this(id, inventory, access, preserving, preserving ? List.of() : GardenOrderReload.current(), null);
    }

    GardenWorkMenu(int id, Inventory inventory, ContainerLevelAccess access, boolean preserving, List<GardenWork.Job> customOrders) {
        this(id, inventory, access, preserving, customOrders, null);
    }

    private GardenWorkMenu(int id, Inventory inventory, ContainerLevelAccess access, boolean preserving,
                           List<GardenWork.Job> customOrders, ContainerData data) {
        super(AEMenus.GARDEN_WORK.get(), id);
        this.inventory = inventory;
        this.access = access;
        this.serverPreserving = preserving;
        this.customOrders = customOrders;
        this.data = data != null ? data : new ContainerData() {
            @Override public int get(int index) {
                if (index >= SHARED_START) {
                    if (preserving || page != SHARED_PAGE || !(inventory.player.level() instanceof ServerLevel level)) return 0;
                    var orders = SharedMarketOrders.get(level);
                    int field = (index - SHARED_START) % SHARED_FIELDS;
                    int row = (index - SHARED_START) / SHARED_FIELDS;
                    return switch (field) {
                        case REWARD -> orders.reward(row, inventory.player.getUUID());
                        case CONTRIBUTED -> orders.contributed(row, inventory.player.getUUID());
                        case ROUND_LOW -> (short) orders.round(row);
                        case ROUND_HIGH -> orders.round(row) >>> 16;
                        default -> orders.remaining(row, field);
                    };
                }
                return switch (index) {
                    case 0 -> preserving ? 1 : 0;
                    case 4 -> page;
                    case 5 -> GardenWork.commissions(inventory.player);
                    case 6 -> GardenWork.commissions(inventory.player, true);
                    default -> GardenWork.completed(inventory.player, index - 1);
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
        addDataSlots(this.data);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, 9 + row * 9 + col, 76 + col * 18, 171 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 76 + col * 18, 229));
    }

    public boolean preserving() { return data.get(0) != 0; }
    public int completed(int row) { return data.get(row + 1); }
    public int page() { return data.get(4); }
    public boolean commissions() { return page() == GardenWork.ROWS || seedCommissions(); }
    public boolean seedCommissions() { return page() == SEED_PAGE; }
    public boolean shared() { return !preserving() && page() == SHARED_PAGE; }
    public int remaining(int row, int ingredient) { return data.get(SHARED_START + row * SHARED_FIELDS + ingredient); }
    public int claimable(int row) { return remaining(row, REWARD); }
    public int contributed(int row) { return remaining(row, CONTRIBUTED); }
    public boolean custom() { return page() >= CUSTOM_PAGE; }
    public int customPages() { return Math.ceilDiv(customOrders.size(), GardenWork.ROWS); }
    public int tier(int row) { return commissions() ? row : page(); }
    public boolean unlocked(int row) {
        if (custom() || shared()) return true;
        int tier = tier(row);
        return preserving() || tier == 0 || completed(tier - 1) >= GardenWork.UNLOCK_AT;
    }
    public boolean claimed(int row) { return commissions() && (data.get(seedCommissions() ? 6 : 5) & (1 << row)) != 0; }
    public GardenWork.Job job(int row) {
        if (shared()) return SharedMarketOrders.ORDERS.get(row);
        if (!custom()) return GardenWork.job(preserving(), page(), row);
        int index = (page() - CUSTOM_PAGE) * GardenWork.ROWS + row;
        return index < customOrders.size() ? customOrders.get(index) : null;
    }
    public boolean ready(int row) {
        var job = job(row);
        return job != null && unlocked(row) && !claimed(row) && job.ready(inventory);
    }

    public boolean canContribute(int row) {
        var ingredients = job(row).ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            if (remaining(row, i) > 0 && SharedMarketOrders.hasDelivery(inventory, ingredients.get(i))) return true;
        }
        return false;
    }

    public boolean canClaim(int row) { return claimable(row) > 0 && SharedMarketOrders.hasRewardSpace(inventory); }
    public int action(int row) { return page() * GardenWork.ROWS + row; }
    public int claimAction(int row) { return SHARED_CLAIM + row; }

    public int contributeAction(int row) {
        // Container data uses signed shorts. Menu button clicks carry the whole packed round and row.
        int round = (remaining(row, ROUND_HIGH) << 16) | (remaining(row, ROUND_LOW) & 0xffff);
        return ~((round << 2) | row);
    }
    public int pageAction(int selected) { return PAGE_BUTTON + selected; }

    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, serverPreserving ? AEBlocks.PRESERVING_STATION.get() : AEBlocks.GARDEN_ORDER_BOARD.get());
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || player.isSpectator() || player != inventory.player || !stillValid(player)) return false;
        int pageCount = CUSTOM_PAGE + customPages();
        if (!serverPreserving && button >= PAGE_BUTTON && button < PAGE_BUTTON + pageCount) {
            page = button - PAGE_BUTTON;
            broadcastChanges();
            return true;
        }
        if (shared() && player.level() instanceof ServerLevel level) {
            var orders = SharedMarketOrders.get(level);
            boolean applied;
            if (button >= SHARED_CLAIM && button < SHARED_CLAIM + GardenWork.ROWS) applied = orders.claim(button - SHARED_CLAIM, player);
            else if (button < 0 && (~button & 3) < GardenWork.ROWS) applied = orders.contribute(~button & 3, ~button >>> 2, player);
            else return false;
            if (applied) broadcastChanges();
            return applied;
        }
        if (button < 0 || button >= GardenWork.ROWS * pageCount || custom() && customOrders != GardenOrderReload.current()) return false;
        int row = button % GardenWork.ROWS;
        if (button != action(row) || !ready(row)) return false;
        int completedPage = page();
        boolean commission = commissions(), seedCommission = seedCommissions(), customOrder = custom();
        if (!job(row).complete(player, () -> {
            if (commission) GardenWork.recordCommission(player, row, seedCommission);
            else if (!serverPreserving && !customOrder) GardenWork.recordOrder(player, completedPage);
            if (serverPreserving) FarmingMilestones.PRESERVE.award(player);
        })) return false;
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = getSlot(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack original = stack.copy();
        if (!(index < 27 ? moveItemStackTo(stack, 27, 36, false) : moveItemStackTo(stack, 0, 27, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
