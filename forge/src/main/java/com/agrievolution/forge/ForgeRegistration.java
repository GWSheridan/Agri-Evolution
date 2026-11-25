package com.agrievolution.forge;

import com.agrievolution.platform.*;

import com.agrievolution.content.GardenToolItem;
import java.util.function.Supplier;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.registries.DeferredRegister;

public record ForgeRegistration(IEventBus modBus) implements RegistrationPlatform {
    @Override
    public <T> void register(RegistrationSet<T> entries) {
        var register = DeferredRegister.create(entries.registryKey(), ModInfo.ID);

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
        return IForgeMenuType.create(factory::create);
    }

    @Override
    public SimpleParticleType simpleParticle(boolean alwaysVisible) {
        return new SimpleParticleType(alwaysVisible) {};
    }

    @Override
    public CreativeModeTab.Builder creativeTab() {
        return CreativeModeTab.builder();
    }

    @Override
    public GardenToolItem gardenTool(Item.Properties properties, boolean trowel, int nutrition) {
        return new GardenToolItem(properties, trowel, nutrition) {
            @Override
            public boolean canPerformAction(ItemStack stack, ToolAction ability) {
                return trowel && ability == ToolActions.HOE_TILL;
            }
        };
    }
}
