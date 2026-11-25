package com.agrievolution.content;

import com.agrievolution.platform.ContentRegistration;
import com.agrievolution.platform.RegistrationSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import java.util.function.Supplier;

public final class AEMenus {
    public static final RegistrationSet<MenuType<?>> MENUS =
            new RegistrationSet<>(Registries.MENU);
    public static final Supplier<MenuType<FarmLedgerMenu>> FARM_LEDGER = MENUS.register("farm_ledger",
            () -> new MenuType<>(FarmLedgerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<IrrigationControllerMenu>> IRRIGATION_CONTROLLER = MENUS.register("irrigation_controller",
            () -> ContentRegistration.platform().extendedMenu(IrrigationControllerMenu::new));
    public static final Supplier<MenuType<BatchCookingMenu>> BATCH_COOKING_STATION = MENUS.register("batch_cooking_station",
            () -> new MenuType<>(BatchCookingMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<GardenWorkMenu>> GARDEN_WORK = MENUS.register("garden_work",
            () -> ContentRegistration.platform().extendedMenu(GardenWorkMenu::new));
    public static final Supplier<MenuType<SeedVaultMenu>> SEED_VAULT = MENUS.register("seed_vault",
            () -> new MenuType<>(SeedVaultMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<LunchBoxMenu>> LUNCH_BOX = MENUS.register("lunch_box",
            () -> ContentRegistration.platform().extendedMenu(
                    (id, inv, buf) -> new LunchBoxMenu(id, inv, buf.readVarInt())));
    public static final Supplier<MenuType<SeedBagMenu>> SEED_BAG = MENUS.register("seed_bag",
            () -> ContentRegistration.platform().extendedMenu(
                    (id, inv, buf) -> new SeedBagMenu(id, inv, buf.readVarInt())));
    public static final Supplier<MenuType<SeedBreedingStationMenu>> SEED_BREEDING_STATION =
            MENUS.register("seed_breeding_station", () -> new MenuType<>(SeedBreedingStationMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<BotanicalGreenhouseMenu>> BOTANICAL_GREENHOUSE =
            MENUS.register("botanical_greenhouse", () -> new MenuType<>(BotanicalGreenhouseMenu::new,
                    FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<VerticalGreenhouseMenu>> VERTICAL_GREENHOUSE =
            MENUS.register("vertical_greenhouse", () -> new MenuType<>(VerticalGreenhouseMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<BotanicalGreenhouseStorageMenu>> BOTANICAL_GREENHOUSE_STORAGE =
            MENUS.register("botanical_greenhouse_storage", () -> new MenuType<>(BotanicalGreenhouseStorageMenu::new,
                    FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<BotanicalGreenhouseHarvestMenu>> BOTANICAL_GREENHOUSE_HARVEST =
            MENUS.register("botanical_greenhouse_harvest", () -> new MenuType<>(BotanicalGreenhouseHarvestMenu::new,
                    FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<BotanicalGreenhouseSeedBankMenu>> BOTANICAL_GREENHOUSE_SEED_BANK =
            MENUS.register("botanical_greenhouse_seed_bank", () -> new MenuType<>(BotanicalGreenhouseSeedBankMenu::new,
                    FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<BotanicalGeneticsLaboratoryMenu>> BOTANICAL_GENETICS_LABORATORY =
            MENUS.register("botanical_genetics_laboratory", () -> new MenuType<>(BotanicalGeneticsLaboratoryMenu::new,
                    FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<SoilResearchTableMenu>> SOIL_RESEARCH_TABLE =
            MENUS.register("soil_research_table", () -> new MenuType<>(SoilResearchTableMenu::new,
                    FeatureFlags.DEFAULT_FLAGS));

    private AEMenus() {}
}
