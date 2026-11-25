package com.agrievolution.client.journal;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/** Font helpers for readable journal text. */
public final class JournalFonts {
    private JournalFonts() {}

    /**
     * Use Minecraft's built-in uniform font for cleaner, less blocky body text.
     * This preserves crisp pixel art while making the journal easier to read.
     */
    public static final ResourceLocation BODY_FONT = ResourceLocation.withDefaultNamespace("uniform");

    public static FormattedCharSequence body(String text) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withFont(BODY_FONT))
                .getVisualOrderText();
    }
}
