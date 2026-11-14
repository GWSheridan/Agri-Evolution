package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class AEEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AgriEvolution.MOD_ID);
    public static final Supplier<EntityType<CropWeedEntity>> CROP_WEED = TYPES.register("crop_weed",
            () -> EntityType.Builder.<CropWeedEntity>of(CropWeedEntity::new, MobCategory.MISC)
                    .sized(.8f, .65f).clientTrackingRange(4).updateInterval(40)
                    .build("agri_evolution:crop_weed"));
    private AEEntities() {}
}
