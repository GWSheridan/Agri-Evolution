package com.agrievolution.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Original garden-workbench artwork. Layout, font scale and hitboxes remain screen-owned. */
public final class GardenGuiTheme {
    private static final ResourceLocation FRAME = texture("garden_frame");
    private static final ResourceLocation HEADER = texture("garden_header");
    private static final ResourceLocation SLOT = texture("seed_tray_slot");
    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("agri_evolution", "textures/gui/garden/" + name + ".png");
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill) {
        if (w <= 0 || h <= 0) return;
        if (w == 18 && h == 18) { slot(g, x + 1, y + 1); return; }
        int r = (fill >>> 16) & 255, green = (fill >>> 8) & 255, blue = fill & 255;
        boolean paper = r >= 150 && green >= 130 && blue >= 90;
        boolean foliage = green > r && green > blue;
        if (r < 75 && green < 75 && w >= 150 && h >= 120) {
            g.fill(x,y,x+w,y+h,fill);
            frame(g,FRAME,x-6,y-6,w+12,h+12,8);
            return;
        }
        int edge = w >= 150 && h >= 120 ? 4 : 2;
        edge = Math.min(edge, Math.min(w,h)/2);
        g.fill(x,y,x+w,y+h,fill);
        ResourceLocation skin = paper ? FRAME : HEADER;
        if (paper || foliage) tile(g, skin, x+edge,y+edge,w-edge*2,h-edge*2,24,24,16,16);
        frame(g, skin, x,y,w,h,edge);
    }

    /** Retain the approved journal pages; add the same botanical binding around them. */
    public static void binding(GuiGraphics g, int x, int y, int w, int h) {
        frame(g,FRAME,x-4,y-4,w+8,h+8,8);
    }

    /** x/y are the inventory item's native 16x16 origin, never a shifted interaction slot. */
    public static void slot(GuiGraphics g, int x, int y) {
        g.blit(SLOT,x-1,y-1,0,0,18,18,18,18);
    }

    private static void frame(GuiGraphics g, ResourceLocation skin, int x, int y, int w, int h, int e) {
        part(g,skin,x,y,e,e,0,0,8,8);
        part(g,skin,x+w-e,y,e,e,56,0,8,8);
        part(g,skin,x,y+h-e,e,e,0,56,8,8);
        part(g,skin,x+w-e,y+h-e,e,e,56,56,8,8);
        part(g,skin,x+e,y,w-2*e,e,8,0,48,8);
        part(g,skin,x+e,y+h-e,w-2*e,e,8,56,48,8);
        part(g,skin,x,y+e,e,h-2*e,0,8,8,48);
        part(g,skin,x+w-e,y+e,e,h-2*e,56,8,8,48);
    }

    private static void tile(GuiGraphics g, ResourceLocation skin, int x, int y, int w, int h,
                             int u, int v, int sw, int sh) {
        for(int dy=0;dy<h;dy+=sh) for(int dx=0;dx<w;dx+=sw) {
            int tw=Math.min(sw,w-dx), th=Math.min(sh,h-dy);
            part(g,skin,x+dx,y+dy,tw,th,u,v,tw,th);
        }
    }
    private static void part(GuiGraphics g, ResourceLocation skin, int x, int y, int w, int h,
                             int u, int v, int sw, int sh) {
        if(w>0 && h>0) g.blit(skin,x,y,w,h,u,v,sw,sh,64,64);
    }
    private GardenGuiTheme() {}
}
