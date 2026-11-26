package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ResourceWorkshopBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    static final int MATERIAL = 0, BINDER = 1, OUTPUT = 2, SLOTS = 3;
    private static final int[] TOP = {MATERIAL}, SIDE = {BINDER}, BOTTOM = {OUTPUT}, NONE = {};

    private final RecipeManager.CachedCheck<ResourceWorkshopRecipe.Input, ResourceWorkshopRecipe> recipes =
            RecipeManager.createCheck(AERecipes.RESOURCE_WORKSHOP.get());
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private ResourceLocation activeId;
    private ResourceWorkshopRecipe activeRecipe;
    private int progress;
    private int duration;
    private Status status = Status.WAITING;

    public enum Status { WAITING, NO_RECIPE, TIER_REQUIRED, PROCESSING, OUTPUT_FULL }

    public ResourceWorkshopBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.RESOURCE_WORKSHOP.get(), pos, state);
    }

    @Override protected Component getDefaultName() { return Component.translatable("block.agri_evolution.resource_workshop"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new ResourceWorkshopMenu(id, inventory, this); }
    @Override public int getContainerSize() { return SLOTS; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> contents) { items = contents; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return (slot == MATERIAL || slot == BINDER) && WorkstationAccess.isCurrent(this);
    }

    @Override public int[] getSlotsForFace(Direction direction) {
        if (!WorkstationAccess.isCurrent(this)) return NONE;
        return switch (direction) {
            case UP -> TOP;
            case DOWN -> BOTTOM;
            default -> SIDE;
        };
    }

    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction != Direction.DOWN && slot == (direction == Direction.UP ? MATERIAL : BINDER) && canPlaceItem(slot, stack);
    }

    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT && direction == Direction.DOWN && WorkstationAccess.isCurrent(this);
    }

    int tier() { return getBlockState().getValue(ResourceWorkshopBlock.TIER); }
    Status status() { return status; }
    int progress() { return progress; }
    int duration() { return duration; }

    void process() {
        if (!(level instanceof ServerLevel server) || !WorkstationAccess.isCurrent(this)) return;
        var material = items.get(MATERIAL);
        var binder = items.get(BINDER);
        if (material.isEmpty() || binder.isEmpty()) {
            reset(Status.WAITING);
            return;
        }
        var input = new ResourceWorkshopRecipe.Input(material, binder);
        var match = recipes.getRecipeFor(input, server).orElse(null);
        if (match == null) {
            reset(Status.NO_RECIPE);
            return;
        }
        var recipe = match.value();
        if (recipe.tier() > tier()) {
            reset(Status.TIER_REQUIRED);
            return;
        }
        if (!match.id().equals(activeId) || duration != recipe.ticks() || activeRecipe != null && activeRecipe != recipe) {
            progress = 0;
            activeId = match.id();
            duration = recipe.ticks();
            setChanged();
        }
        activeRecipe = recipe;
        var output = items.get(OUTPUT);
        if (!recipe.fitsOutput(output)) {
            status = Status.OUTPUT_FULL;
            return;
        }
        status = Status.PROCESSING;
        if (++progress < duration) {
            if (progress % 20 == 0) setChanged();
            return;
        }
        items.set(MATERIAL, material.copyWithCount(material.getCount() - recipe.materialCount()));
        items.set(BINDER, binder.copyWithCount(binder.getCount() - recipe.binderCount()));
        var result = recipe.assemble(input, server.registryAccess());
        result.grow(output.getCount());
        items.set(OUTPUT, result);
        progress = 0;
        setChanged();
    }

    private void reset(Status nextStatus) {
        status = nextStatus;
        if (activeId == null) return;
        activeId = null;
        activeRecipe = null;
        progress = 0;
        duration = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (activeId != null) {
            tag.putString("Recipe", activeId.toString());
            tag.putInt("Duration", duration);
            tag.putInt("Progress", progress);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        duration = Math.clamp(tag.getInt("Duration"), 0, ResourceWorkshopRecipe.MAX_TICKS);
        activeId = duration == 0 ? null : ResourceLocation.tryParse(tag.getString("Recipe"));
        if (activeId == null) duration = 0;
        progress = duration == 0 ? 0 : Math.clamp(tag.getInt("Progress"), 0, duration - 1);
        activeRecipe = null;
        status = Status.WAITING;
    }
}
