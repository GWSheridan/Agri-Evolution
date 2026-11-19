package com.agrievolution.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Crouching skips Block#useItemOn in 1.21.1, so handle this before item placement. */
public final class IrrigationBasinUpgradeHandler {
    private IrrigationBasinUpgradeHandler() {}

    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;
        Level world = event.getLevel();
        if (!world.hasChunkAt(event.getPos())) return;
        if (!(world.getBlockState(event.getPos()).getBlock() instanceof IrrigationBasinBlock)) return;
        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(Items.COPPER_BLOCK) && !held.is(Items.AMETHYST_BLOCK)) return;

        // Consume the interaction on both sides. Otherwise vanilla tries to place the block.
        event.setCancellationResult(InteractionResult.sidedSuccess(world.isClientSide));
        event.setCanceled(true);
        if (world.isClientSide) return;
        if (!WorkstationAccess.mayUse(world, event.getPos(), player, event.getHitVec().getDirection(), held)) return;
        if (!(world.getBlockEntity(event.getPos()) instanceof IrrigationBasinBlockEntity basin)) return;
        int current = basin.upgradeTier();
        boolean correct = (current == 0 && held.is(Items.COPPER_BLOCK))
                || (current == 1 && held.is(Items.AMETHYST_BLOCK));
        if (!correct) {
            player.displayClientMessage(Component.literal(current >= 2 ? "Basin already Mk III."
                    : "Basin Mk " + (current + 1) + " requires "
                    + (current == 0 ? "a Copper Block" : "an Amethyst Block") + " next."), true);
            return;
        }
        if (!basin.upgradeTo(current + 1)) return;
        if (!player.getAbilities().instabuild) held.shrink(1);
        player.displayClientMessage(Component.literal("Irrigation Basin upgraded to Mk "
                + (current + 2) + "! Watering radius: " + basin.wateringRadius()
                + " blocks."), true);
    }
}
