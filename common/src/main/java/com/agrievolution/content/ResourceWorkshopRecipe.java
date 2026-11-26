package com.agrievolution.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record ResourceWorkshopRecipe(Ingredient material, int materialCount, Ingredient binder, int binderCount,
                                     ItemStack result, int tier, int ticks) implements Recipe<ResourceWorkshopRecipe.Input> {
    static final int MAX_TICKS = 12000;

    public ResourceWorkshopRecipe {
        if (material.isEmpty() || binder.isEmpty() || materialCount < 1 || materialCount > 64
                || binderCount < 1 || binderCount > 64 || result.isEmpty() || result.getCount() > result.getMaxStackSize()
                || tier < 1 || tier > 3 || ticks < 1 || ticks > MAX_TICKS) {
            throw new IllegalArgumentException("Invalid Resource Workshop recipe");
        }
        result = result.copy();
    }

    @Override public ItemStack result() { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result(); }
    @Override public ItemStack assemble(Input input, HolderLookup.Provider registries) { return result(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeType<?> getType() { return AERecipes.RESOURCE_WORKSHOP.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return AERecipes.RESOURCE_WORKSHOP_SERIALIZER.get(); }

    @Override
    public boolean matches(Input input, Level level) {
        return input.material().getCount() >= materialCount && material.test(input.material())
                && input.binder().getCount() >= binderCount && binder.test(input.binder());
    }

    boolean fitsOutput(ItemStack output) {
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() <= output.getMaxStackSize() - result.getCount();
    }

    public record Input(ItemStack material, ItemStack binder) implements RecipeInput {
        @Override public int size() { return 2; }
        @Override public ItemStack getItem(int slot) {
            return switch (slot) {
                case 0 -> material;
                case 1 -> binder;
                default -> throw new IndexOutOfBoundsException(slot);
            };
        }
    }

    static final class Serializer implements RecipeSerializer<ResourceWorkshopRecipe> {
        private static final MapCodec<ResourceWorkshopRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(ResourceWorkshopRecipe::material),
                Codec.intRange(1, 64).optionalFieldOf("material_count", 1).forGetter(ResourceWorkshopRecipe::materialCount),
                Ingredient.CODEC_NONEMPTY.fieldOf("binder").forGetter(ResourceWorkshopRecipe::binder),
                Codec.intRange(1, 64).optionalFieldOf("binder_count", 1).forGetter(ResourceWorkshopRecipe::binderCount),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.intRange(1, 3).fieldOf("tier").forGetter(ResourceWorkshopRecipe::tier),
                Codec.intRange(1, MAX_TICKS).fieldOf("ticks").forGetter(ResourceWorkshopRecipe::ticks)
        ).apply(builder, ResourceWorkshopRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ResourceWorkshopRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override public MapCodec<ResourceWorkshopRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ResourceWorkshopRecipe> streamCodec() { return STREAM_CODEC; }

        private static void encode(RegistryFriendlyByteBuf buffer, ResourceWorkshopRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.material);
            buffer.writeVarInt(recipe.materialCount);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.binder);
            buffer.writeVarInt(recipe.binderCount);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeVarInt(recipe.tier);
            buffer.writeVarInt(recipe.ticks);
        }

        private static ResourceWorkshopRecipe decode(RegistryFriendlyByteBuf buffer) {
            return new ResourceWorkshopRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt(),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt(), ItemStack.STREAM_CODEC.decode(buffer),
                    buffer.readVarInt(), buffer.readVarInt());
        }
    }
}
