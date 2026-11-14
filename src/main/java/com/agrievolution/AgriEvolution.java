package com.agrievolution;

import com.agrievolution.content.AEItems;
import com.agrievolution.content.AEBlocks;
import com.agrievolution.content.AEBlockEntities;
import com.agrievolution.content.AECreativeTabs;
import com.agrievolution.content.AEMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(AgriEvolution.MOD_ID)
public final class AgriEvolution {
    public static final String MOD_ID = "agri_evolution";

    public AgriEvolution(IEventBus modEventBus) {
        AEBlocks.BLOCKS.register(modEventBus);
        AEBlockEntities.TYPES.register(modEventBus);
        AEItems.ITEMS.register(modEventBus);
        AEMenus.MENUS.register(modEventBus);
        com.agrievolution.content.AEParticles.TYPES.register(modEventBus);
        com.agrievolution.content.AEEntities.TYPES.register(modEventBus);
        AECreativeTabs.TABS.register(modEventBus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.agrievolution.content.SpannerItem::onRightClick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.agrievolution.content.SpannerItem::onLeftClick);
        // A crouching player bypasses normal block useItemOn in MC 1.21.1.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.agrievolution.content.IrrigationBasinUpgradeHandler::onRightClick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.agrievolution.content.StarterProgressionEvents::onGrassDrops);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                com.agrievolution.content.StarterProgressionEvents::onPlayerJoin);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.agrievolution.content.StarterProgressionEvents::onClone);
    }
}
