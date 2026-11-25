package com.agrievolution.content;

import com.agrievolution.platform.RegistrationSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.function.Supplier;

public final class AEEntities {
    public static final RegistrationSet<EntityType<?>> TYPES = new RegistrationSet<>(Registries.ENTITY_TYPE);
    public static final Supplier<EntityType<CropWeedEntity>> CROP_WEED = TYPES.register("crop_weed",
            () -> EntityType.Builder.<CropWeedEntity>of(CropWeedEntity::new, MobCategory.MISC)
                    .sized(.8f, .65f).clientTrackingRange(4).updateInterval(40)
                    .build("agri_evolution:crop_weed"));
    private AEEntities() {}
}
