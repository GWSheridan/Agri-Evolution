package com.agrievolution;

import com.agrievolution.platform.ContentRegistration;
import com.agrievolution.platform.ModInfo;
import com.agrievolution.platform.Network;
import com.agrievolution.platform.Game;
import com.agrievolution.platform.NeoForgeEvents;
import com.agrievolution.platform.NeoForgeGameHooks;
import com.agrievolution.platform.NeoForgeNetwork;
import com.agrievolution.platform.NeoForgeRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(AgriEvolution.MOD_ID)
public final class AgriEvolution {
    public static final String MOD_ID = ModInfo.ID;

    public AgriEvolution(IEventBus modEventBus) {
        Game.install(new NeoForgeGameHooks());
        Network.install(new NeoForgeNetwork());
        modEventBus.addListener(NeoForgeNetwork::register);
        ContentRegistration.register(new NeoForgeRegistration(modEventBus));
        NeoForgeEvents.register();
    }
}
