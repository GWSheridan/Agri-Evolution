package com.agrievolution.content;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class FarmLedgerItem extends Item {
    public FarmLedgerItem(Properties properties) { super(properties); }
    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inv, who) -> new FarmLedgerMenu(id, inv), Component.literal("Farm Ledger")));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            if (player.isShiftKeyDown()) { FarmLedgerService.cancelMarking(server); player.displayClientMessage(Component.literal("Field marking cancelled."), true); }
            else open(server);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        FarmLedgerService.useOn(context); return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Plan fields, count seeds and check growing conditions."));
        lines.add(Component.literal("Right-click: open. Use on a machine: link to selected field."));
        lines.add(Component.literal("Sneak-use: cancel marking. Plans stay in the world save."));
    }
}
