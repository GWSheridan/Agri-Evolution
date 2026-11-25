package com.agrievolution.platform;

import com.agrievolution.content.GardenToolItem;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public interface RegistrationPlatform {
    <T> void register(RegistrationSet<T> entries);

    <T extends AbstractContainerMenu> MenuType<T> extendedMenu(MenuFactory<T> factory);

    SimpleParticleType simpleParticle(boolean alwaysVisible);

    CreativeModeTab.Builder creativeTab();

    GardenToolItem gardenTool(Item.Properties properties, boolean trowel, int nutrition);

    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory inventory, FriendlyByteBuf data);
    }
}
