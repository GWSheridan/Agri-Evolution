package com.agrievolution.content;

import com.agrievolution.platform.ModInfo;
import com.agrievolution.platform.RegistrationSet;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class AERecipes {
    public static final RegistrationSet<RecipeType<?>> TYPES = new RegistrationSet<>(Registries.RECIPE_TYPE);
    public static final RegistrationSet<RecipeSerializer<?>> SERIALIZERS = new RegistrationSet<>(Registries.RECIPE_SERIALIZER);
    public static final Supplier<RecipeType<ResourceWorkshopRecipe>> RESOURCE_WORKSHOP = TYPES.register(
            "resource_workshop", () -> new RecipeType<ResourceWorkshopRecipe>() {
                @Override public String toString() { return ModInfo.id("resource_workshop").toString(); }
            });
    public static final Supplier<RecipeSerializer<ResourceWorkshopRecipe>> RESOURCE_WORKSHOP_SERIALIZER = SERIALIZERS.register(
            "resource_workshop", ResourceWorkshopRecipe.Serializer::new);

    private AERecipes() {}
}
