package com.agrievolution.platform;

import com.agrievolution.content.*;

public final class ContentRegistration {
    private static RegistrationPlatform platform;

    private ContentRegistration() {}

    public static void register(RegistrationPlatform implementation) {
        if (platform != null) throw new IllegalStateException("Content already registered");
        platform = implementation;
        platform.register(AEBlocks.BLOCKS);
        platform.register(AEBlockEntities.TYPES);
        platform.register(AEItems.ITEMS);
        platform.register(AERecipes.TYPES);
        platform.register(AERecipes.SERIALIZERS);
        platform.register(AEMenus.MENUS);
        platform.register(AEParticles.TYPES);
        platform.register(AEEntities.TYPES);
        platform.register(AECreativeTabs.TABS);
    }

    public static RegistrationPlatform platform() {
        if (platform == null) throw new IllegalStateException("Registration platform has not started");
        return platform;
    }
}
