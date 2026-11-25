package com.agrievolution.client.journal;

import com.agrievolution.platform.ModInfo;
import net.minecraft.resources.ResourceLocation;

/** Shared screen measurements and artwork references. All sizes are GUI pixels. */
public final class JournalTheme {
    private JournalTheme() {}
    public static final int WIDTH = 430;
    public static final int HEIGHT = 270;
    public static final int INK = 0x3D2B1E;
    public static final int MUTED = 0x806851;
    public static final int GREEN = 0x305938;
    public static final int GOLD = 0xB88A3B;
    public static final int RED = 0x9E3F47;
    public static final int BLUE = 0x3766A2;
    public static final int PURPLE = 0x7848A0;
    public static final int LOCKED = 0x78746D;

    public static ResourceLocation texture(String file) {
        return ResourceLocation.fromNamespaceAndPath(ModInfo.ID,
                "textures/gui/journal/" + file + ".png");
    }
}
