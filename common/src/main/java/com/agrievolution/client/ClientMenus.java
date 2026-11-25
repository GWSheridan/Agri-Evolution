package com.agrievolution.client;

import com.agrievolution.client.breeding.SeedBreedingStationScreen;
import com.agrievolution.client.greenhouse.*;
import com.agrievolution.client.laboratory.BotanicalGeneticsLaboratoryScreen;
import com.agrievolution.client.soil.SoilResearchTableScreen;
import com.agrievolution.content.AEMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class ClientMenus {
    private ClientMenus() {}

    public static void register(Registrar screens) {
        screens.register(AEMenus.FARM_LEDGER.get(), FarmLedgerScreen::new);
        screens.register(AEMenus.SEED_BAG.get(), SeedBagScreen::new);
        screens.register(AEMenus.LUNCH_BOX.get(), LunchBoxScreen::new);
        screens.register(AEMenus.GARDEN_WORK.get(), GardenWorkScreen::new);
        screens.register(AEMenus.SEED_VAULT.get(), SeedVaultScreen::new);
        screens.register(AEMenus.IRRIGATION_CONTROLLER.get(), IrrigationControllerScreen::new);
        screens.register(AEMenus.BATCH_COOKING_STATION.get(), BatchCookingScreen::new);
        screens.register(AEMenus.SEED_BREEDING_STATION.get(), SeedBreedingStationScreen::new);
        screens.register(AEMenus.BOTANICAL_GREENHOUSE.get(), BotanicalGreenhouseScreen::new);
        screens.register(AEMenus.VERTICAL_GREENHOUSE.get(), VerticalGreenhouseScreen::new);
        screens.register(AEMenus.BOTANICAL_GREENHOUSE_STORAGE.get(), BotanicalGreenhouseStorageScreen::new);
        screens.register(AEMenus.BOTANICAL_GREENHOUSE_HARVEST.get(), BotanicalGreenhouseHarvestScreen::new);
        screens.register(AEMenus.BOTANICAL_GREENHOUSE_SEED_BANK.get(), BotanicalGreenhouseSeedBankScreen::new);
        screens.register(AEMenus.BOTANICAL_GENETICS_LABORATORY.get(), BotanicalGeneticsLaboratoryScreen::new);
        screens.register(AEMenus.SOIL_RESEARCH_TABLE.get(), SoilResearchTableScreen::new);
    }

    public interface Registrar {
        <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(
                MenuType<M> type, MenuScreens.ScreenConstructor<M, S> screen);
    }
}
