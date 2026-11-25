package com.agrievolution.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Crouching skips Block#useItemOn in 1.21.1, so handle this before item placement. */
public final class IrrigationBasinUpgradeHandler {
    private IrrigationBasinUpgradeHandler() {}

    public static InteractionResult use(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        var pos = hit.getBlockPos();
        if (!world.hasChunkAt(pos)) return InteractionResult.PASS;
        if (!(world.getBlockState(pos).getBlock() instanceof IrrigationBasinBlock)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.COPPER_BLOCK) && !held.is(Items.AMETHYST_BLOCK)) return InteractionResult.PASS;

        // Consume the interaction on both sides. Otherwise vanilla tries to place the block.
        var handled = InteractionResult.sidedSuccess(world.isClientSide);
        if (world.isClientSide) return handled;
        var state = world.getBlockState(pos);
        if (!(world.getBlockEntity(pos) instanceof IrrigationBasinBlockEntity basin)) return handled;
        var before = held.copy();
        if (!WorkstationAccess.mayCompleteItemUse(world, player, hand, held, before)
                || !WorkstationAccess.mayUse(world, pos, player, hit.getDirection(), held)
                || !WorkstationAccess.mayCompleteItemUse(world, player, hand, held, before)
                || !player.isShiftKeyDown() || basin.getLevel() != world || !WorkstationAccess.isCurrent(basin)
                || world.getBlockState(pos) != state) return handled;
        int current = basin.upgradeTier();
        boolean correct = (current == 0 && held.is(Items.COPPER_BLOCK))
                || (current == 1 && held.is(Items.AMETHYST_BLOCK));
        if (!correct) {
            player.displayClientMessage(Component.literal(current >= 2 ? "Basin already Mk III."
                    : "Basin Mk " + (current + 1) + " requires "
                    + (current == 0 ? "a Copper Block" : "an Amethyst Block") + " next."), true);
            return handled;
        }
        if (!basin.upgradeTo(current + 1)) return handled;
        if (!player.getAbilities().instabuild) held.shrink(1);
        player.displayClientMessage(Component.literal("Irrigation Basin upgraded to Mk "
                + (current + 2) + "! Watering radius: " + basin.wateringRadius()
                + " blocks."), true);
        return handled;
    }
}
