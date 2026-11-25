package com.agrievolution.forge;

import com.agrievolution.platform.ContentRegistration;
import com.agrievolution.platform.Game;
import com.agrievolution.platform.ModInfo;
import com.agrievolution.platform.Network;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ModInfo.ID)
public final class AgriEvolutionForge {
    public AgriEvolutionForge(FMLJavaModLoadingContext context) {
        var bus = context.getModEventBus();
        Game.install(new ForgeGameHooks());
        var network = new ForgeNetwork();
        Network.install(network);
        ContentRegistration.register(new ForgeRegistration(bus));
        ForgeReservoirPort.TYPES.register(bus);
        ForgeLoot.CODECS.register(bus);
        ForgeEvents.register();
    }
}
