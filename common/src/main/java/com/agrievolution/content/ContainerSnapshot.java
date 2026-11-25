package com.agrievolution.content;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Detects slot replacements and in-place edits across calls into other mods. */
final class ContainerSnapshot {
    private final Container container;
    private final ItemStack[] references, contents;

    ContainerSnapshot(Container container) {
        this.container = container;
        references = new ItemStack[container.getContainerSize()];
        contents = new ItemStack[references.length];
        for (int slot = 0; slot < references.length; slot++) {
            references[slot] = container.getItem(slot);
            contents[slot] = references[slot].copy();
        }
    }

    boolean current() {
        if (container.getContainerSize() != references.length) return false;
        for (int slot = 0; slot < references.length; slot++) {
            if (container.getItem(slot) != references[slot] || !ItemStack.matches(contents[slot], references[slot])) return false;
        }
        return true;
    }
}
