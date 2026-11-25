package com.agrievolution.preview;

import com.agrievolution.client.journal.JournalFonts;
import com.agrievolution.client.journal.MachineGuidePages;
import net.minecraft.client.gui.Font;

/** Measures guide text after the client has loaded its real font resources. */
final class JournalLayoutChecks {
    static void verify(Font font) {
        int lines = 0;
        for (int index = 0; index < MachineGuidePages.count(); index++) {
            var page = MachineGuidePages.get(index);
            check(font, page.title(), page.title());
            check(font, page.title(), page.subtitle());
            for (var column : new String[][]{page.left(), page.right()}) {
                if (column.length > 6) throw new AssertionError("Journal column too tall: " + page.title());
                for (String line : column) { check(font, page.title(), line); lines++; }
            }
        }
        System.out.println("JOURNAL_LAYOUT_CHECK: " + lines + " body lines and all headings fit the 155-pixel columns");
    }

    private static void check(Font font, String page, String text) {
        int width = font.width(JournalFonts.body(text));
        if (width > 155) throw new AssertionError("Journal text exceeds 155 pixels: " + page + " / " + text + " (" + width + ")");
    }
}
