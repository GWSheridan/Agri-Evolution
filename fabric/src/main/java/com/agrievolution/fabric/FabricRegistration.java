package com.agrievolution.fabric;

import com.agrievolution.content.GardenToolItem;
import com.agrievolution.platform.RegistrationPlatform;
import com.agrievolution.platform.RegistrationSet;
import io.netty.buffer.Unpooled;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

final class FabricRegistration implements RegistrationPlatform {
    static final int MAX_MENU_DATA = 2048;
    private static final StreamCodec<RegistryFriendlyByteBuf, byte[]> MENU_DATA =
            StreamCodec.of((buf, bytes) -> buf.writeByteArray(bytes), buf -> buf.readByteArray(MAX_MENU_DATA));

    @Override
    public <T> void register(RegistrationSet<T> declarations) {
        @SuppressWarnings("unchecked")
        var registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(declarations.registryKey().location());
        if (registry == null) throw new IllegalArgumentException("Missing registry: " + declarations.registryKey());
        declarations.bind(new RegistrationSet.Binder<T>() {
            @Override public <V extends T> Supplier<V> register(ResourceLocation id, Supplier<V> factory) {
                V value = Registry.register(registry, id, factory.get());
                return () -> value;
            }
        });
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> extendedMenu(MenuFactory<T> factory) {
        return new ExtendedScreenHandlerType<>((id, inventory, bytes) -> {
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            try { return factory.create(id, inventory, buffer); }
            finally { buffer.release(); }
        }, MENU_DATA);
    }

    @Override public SimpleParticleType simpleParticle(boolean visible) { return FabricParticleTypes.simple(visible); }
    @Override public CreativeModeTab.Builder creativeTab() { return FabricItemGroup.builder(); }
    @Override public GardenToolItem gardenTool(Item.Properties properties, boolean trowel, int nutrition) {
        return new GardenToolItem(properties, trowel, nutrition);
    }
}
