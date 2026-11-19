package com.agrievolution.content;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/** One persistent bag; empty/partial/full are visual states, never replacement items. */
public final class SeedBagItem extends Item {
    public static final int SLOTS = 9;
    public SeedBagItem(Properties properties) { super(properties); }
    public static boolean accepts(ItemStack stack) {
        return SeedBreedingParents.accepts(stack);
    }
    public static int occupied(ItemStack bag) {
        return (int) bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyStream().count();
    }
    public static float fillState(ItemStack bag) {
        int slots = occupied(bag);
        return slots == 0 ? 0 : slots >= SLOTS ? 1 : 0.5f;
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable(occupied(stack) == 0 ? "item.agri_evolution.seed_bag_empty"
                : occupied(stack) >= SLOTS ? "item.agri_evolution.seed_bag_full" : "item.agri_evolution.seed_bag");
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            server.openMenu(new SimpleMenuProvider((id, inv, who) -> new SeedBagMenu(id, inv, slot),
                    Component.translatable("container.agri_evolution.seed_bag")), buf -> buf.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(bag, level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(occupied(stack) + "/9 slots occupied • Seeds only"));
        lines.add(Component.literal("Right-click to open. Preserves seed genetics."));
    }
}
