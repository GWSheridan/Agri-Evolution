package com.agrievolution.client.soil;

import com.agrievolution.client.GardenGuiTheme;
import com.agrievolution.farming.ResourceCropRules;
import com.agrievolution.genetics.CropSpecies;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Browses the same catalyst rules used by living resource crops. No world or inventory actions. */
public final class CropCareGuideScreen extends Screen {
    private final Screen parent;
    private final String initial;
    private final List<CropSpecies> all=Arrays.stream(CropSpecies.values()).filter(CropSpecies::isResource).toList();
    private List<CropSpecies> matches=all;
    private EditBox search;
    private String query="";
    private int page,x,y,w,h,scroll;
    private boolean initialized;
    public CropCareGuideScreen(Screen parent,String initial) {
        super(Component.literal("Resource crop catalysts"));this.parent=parent;this.initial=initial;
    }
    @Override protected void init() {
        w=Math.min(440,width-24);h=Math.min(260,height-24);x=(width-w)/2;y=(height-h)/2;
        search=addRenderableWidget(new EditBox(font,x+14,y+43,w-28,18,Component.literal("Find a resource crop")));
        search.setHint(Component.literal("Find a resource crop"));
        search.setMaxLength(64);search.setValue(query);search.setResponder(value->{query=value;filter();});
        if(!initialized) {initialized=true;for(int i=0;i<all.size();i++)if(all.get(i).id().equals(initial))page=i;}
        addRenderableWidget(GardenGuiTheme.button(Component.literal("<"),x+14,y+69,26,18,b->move(-1)));
        addRenderableWidget(GardenGuiTheme.button(Component.literal(">"),x+44,y+69,26,18,b->move(1)));
        addRenderableWidget(GardenGuiTheme.button(Component.literal("Back"),x+w-66,y+h-27,52,18,b->onClose()));
    }
    private void filter() {
        String needle=query.toLowerCase(Locale.ROOT).strip();
        matches=all.stream().filter(c->c.displayName().toLowerCase(Locale.ROOT).contains(needle)||c.id().contains(needle)).toList();
        page=0;scroll=0;
    }
    private void move(int direction){if(!matches.isEmpty())page=Math.floorMod(page+direction,matches.size());scroll=0;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta) {
        super.renderBackground(g,mx,my,delta);
        GardenGuiTheme.panel(g,x,y,w,h,0xffeeddb6);
        GardenGuiTheme.panel(g,x+7,y+7,w-14,27,0xff275841);
        g.drawString(font,title,x+14,y+17,0xfff0dfbb,false);
        g.drawString(font,(matches.isEmpty()?0:page+1)+" / "+matches.size()+" resources",x+80,y+74,0xff275841,false);
        if(matches.isEmpty())g.drawString(font,"No matching resource crops.",x+14,y+100,0xff35251b,false);
        else {
            var crop=matches.get(Math.min(page,matches.size()-1));
            g.drawString(font,font.plainSubstrByWidth(crop.displayName(),w-28),x+14,y+99,0xff275841,false);
            var lines=font.split(Component.literal(ResourceCropRules.catalystHint(crop)),w-28);
            int visible=Math.max(1,(h-181)/10);
            scroll=Math.clamp(scroll,0,Math.max(0,lines.size()-visible));
            for(int i=0;i<visible && i+scroll<lines.size();i++)g.drawString(font,lines.get(i+scroll),x+14,y+117+i*10,0xff35251b,false);
            if(lines.size()>visible)g.drawString(font,"Scroll for more",x+14,y+h-76,0xff856d55,false);
        }
        g.drawString(font,"Crop / farmland / catalyst directly below",x+14,y+h-62,0xff275841,false);
        g.drawString(font,"Optional bonus. The block is not consumed.",x+14,y+h-46,0xff35251b,false);
    }
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){scroll-=Math.signum(dy);return true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
