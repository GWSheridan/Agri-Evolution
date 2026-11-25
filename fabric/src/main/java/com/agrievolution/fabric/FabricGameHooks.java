package com.agrievolution.fabric;

import com.agrievolution.platform.GameHooks;
import com.agrievolution.platform.ItemOutput;
import com.agrievolution.platform.ModInfo;
import com.agrievolution.platform.WaterTicket;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class FabricGameHooks implements GameHooks {
    private static final AttachmentType<CompoundTag> PROGRESS = AttachmentRegistry.create(ModInfo.id("progress"),
            builder -> builder.initializer(CompoundTag::new).persistent(CompoundTag.CODEC));

    @Override public CompoundTag persistentData(Entity entity) { return entity.getAttachedOrCreate(PROGRESS); }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData) {
        player.openMenu(new ExtendedScreenHandlerFactory<byte[]>() {
            @Override public byte[] getScreenOpeningData(ServerPlayer holder) {
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    extraData.accept(buffer);
                    int size = buffer.readableBytes();
                    if (size > FabricRegistration.MAX_MENU_DATA) throw new IllegalArgumentException("Menu data is too large");
                    var bytes = new byte[size];
                    buffer.readBytes(bytes);
                    return bytes;
                } finally { buffer.release(); }
            }
            @Override public Component getDisplayName() { return provider.getDisplayName(); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player holder) {
                return provider.createMenu(id, inventory, holder);
            }
        });
    }

    @Override public FoodProperties food(ItemStack stack, Entity consumer) { return stack.get(DataComponents.FOOD); }
    @Override public boolean hasCraftingRemainder(ItemStack stack) { return stack.getItem().hasCraftingRemainingItem(); }

    @Override
    public ItemOutput itemOutput(ServerLevel level, BlockPos pos, Direction face) {
        var storage = ItemStorage.SIDED.find(level, pos, face);
        if (storage == null) return null;
        return new ItemOutput() {
            @Override public int slots() { return storage instanceof SlottedStorage<?> slotted ? slotted.getSlotCount() : 1; }
            @Override public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
                Storage<ItemVariant> target = storage;
                if (storage instanceof SlottedStorage<ItemVariant> slotted) target = slotted.getSlot(slot);
                return offer(target, stack, simulate);
            }
            @Override public ItemStack insertStacked(ItemStack stack) { return offer(storage, stack, false); }
        };
    }

    private static ItemStack offer(Storage<ItemVariant> storage, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (var tx = Transaction.openOuter()) {
            int inserted = Math.toIntExact(storage.insert(ItemVariant.of(stack), stack.getCount(), tx));
            if (!simulate) tx.commit();
            return stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    @Override public void invalidateTransfers(Level level, BlockPos pos) {
        // Fabric queries storage providers against the live block entity and route.
    }
    @Override public WaterTicket waterTicket(ServerLevel level, AABB area) {
        return FabricWaterTickets.register(level, area);
    }
    @Override
    public WaterTicket waterTicket(ServerLevel level, Predicate<Vec3> covers, ChunkPos origin, ChunkPos[] additional) {
        List<ChunkPos> chunks = new ArrayList<>(1 + additional.length);
        chunks.add(origin);
        chunks.addAll(List.of(additional));
        return FabricWaterTickets.register(level, covers, chunks);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, BlockPos neighbor) {}
    @Override public boolean capturingPlacement(Level level) { return false; }
    @Override public void onBlockEntityLoad(BlockEntity entity) {}

    @Override public BlockState tillingState(BlockState state, UseOnContext context) {
        if (context.getClickedFace() == Direction.DOWN) return null;
        if (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) return Blocks.DIRT.defaultBlockState();
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH))
            return Blocks.FARMLAND.defaultBlockState();
        return null;
    }
}
