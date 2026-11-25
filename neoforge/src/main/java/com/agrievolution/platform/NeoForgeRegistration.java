package com.agrievolution.platform;

import com.agrievolution.content.GardenToolItem;
import java.util.function.Supplier;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.registries.DeferredRegister;

public record NeoForgeRegistration(IEventBus modBus) implements RegistrationPlatform {
    @Override
    public <T> void register(RegistrationSet<T> entries) {
        var register = DeferredRegister.create(entries.registryKey(), ModInfo.ID);
        entries.aliases().forEach(register::addAlias);
        entries.bind(new RegistrationSet.Binder<T>() {
            @Override
            public <V extends T> Supplier<V> register(ResourceLocation id, Supplier<V> factory) {
                return register.register(id.getPath(), factory);
            }
        });
        register.register(modBus);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> extendedMenu(MenuFactory<T> factory) {
        return IMenuTypeExtension.create(factory::create);
    }

    @Override
    public SimpleParticleType simpleParticle(boolean alwaysVisible) {
        return new SimpleParticleType(alwaysVisible);
    }

    @Override
    public CreativeModeTab.Builder creativeTab() {
        return CreativeModeTab.builder();
    }

    @Override
    public GardenToolItem gardenTool(Item.Properties properties, boolean trowel, int nutrition) {
        return new GardenToolItem(properties, trowel, nutrition) {
            @Override
            public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
                return trowel && ability == ItemAbilities.HOE_TILL;
            }
        };
    }
}
