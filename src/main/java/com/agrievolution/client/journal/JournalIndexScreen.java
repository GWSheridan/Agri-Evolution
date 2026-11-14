package com.agrievolution.client.journal;

import java.util.*;
import com.agrievolution.client.GardenGuiTheme;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact contents, text search and discovery tracker shared by both books. */
public final class JournalIndexScreen extends Screen {
    private final JournalScreen parent;
    private final JournalNavigation nav;
    private final JournalSnapshot snapshot;
    private final JournalBookmarks marks;
    private final List<JournalIndex.Entry> entries;
    private EditBox search;
    private boolean starred,progress,pending;
    private int page,left,top,w,h,rows;
    private String query="";
    private List<JournalIndex.Entry> filtered=List.of();
    public JournalIndexScreen(JournalScreen parent,JournalNavigation nav,JournalSnapshot snapshot){
        super(Component.literal("Journal contents"));this.parent=parent;this.nav=nav;this.snapshot=snapshot;
        entries=JournalIndex.entries(nav.building());marks=new JournalBookmarks(nav.building());
    }
    @Override protected void init(){
        w=Math.min(420,width-16);h=Math.min(260,height-16);left=(width-w)/2;top=(height-h)/2;
        rows=Math.max(1,(h-104)/29);
        search=new EditBox(font,left+10,top+26,w-20,18,Component.literal("Search this journal"));
        search.setMaxLength(120);search.setValue(query);search.setHint(Component.literal("Search pages, recipes and ingredients"));
        search.setResponder(value->{query=value;page=0;refresh();});
        refresh();
    }
    private void button(String label,int x,int y,int width,Runnable action){addRenderableWidget(new GardenButton(x,y,width,label,action));}
    private void refresh(){
        boolean typing=search.isFocused();clearWidgets();addRenderableWidget(search);if(typing)setFocused(search);
        filtered=entries.stream().filter(e->e.matches(query)).filter(e->!starred||marks.contains(e.key()))
            .filter(e->!progress||e.section()==JournalNavigation.Section.BREEDING&&BreedingGuidePages.recipe(e.page())!=null)
            .filter(e->!progress||!pending||!snapshot.entry(BreedingGuidePages.recipe(e.page()).result()).discovered()).toList();
        page=Math.clamp(page,0,Math.max(0,(filtered.size()-1)/rows));
        int tabWidth=nav.building()?100:(w-32)/4;
        button(starred?"Bookmarks *":"All pages",left+10,top+49,tabWidth,()->{starred=!starred;page=0;refresh();});
        if(!nav.building()){
            button(progress?"Contents":tabWidth>=105?"Breeding progress":"Progress",left+14+tabWidth,top+49,tabWidth,()->{progress=!progress;page=0;refresh();});
            if(progress)button(pending?"Pending":"All results",left+18+2*tabWidth,top+49,tabWidth,()->{pending=!pending;page=0;refresh();});
            else button("Plan a crop",left+18+2*tabWidth,top+49,tabWidth,()->minecraft.setScreen(new BreedingPlannerScreen(this,snapshot)));
            button("Varieties",left+22+3*tabWidth,top+49,tabWidth,
                    ()->minecraft.setScreen(new SeedVarietyScreen(this,snapshot.varieties())));
        }
        for(int r=0;r<rows&&page*rows+r<filtered.size();r++){
            var entry=filtered.get(page*rows+r);int y=top+73+r*29;
            button(font.plainSubstrByWidth(entry.title(),w-64),left+10,y,w-48,()->{entry.open(nav);minecraft.setScreen(parent);});
            button(marks.contains(entry.key())?"*":"+",left+w-34,y,24,()->{marks.toggle(entry.key());refresh();});
        }
        button("<",left+10,top+h-25,24,()->{page--;refresh();});
        button(">",left+38,top+h-25,24,()->{page++;refresh();});
        button("Back",left+w-60,top+h-25,50,this::onClose);
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){
        super.renderBackground(g,mx,my,delta);
        GardenGuiTheme.panel(g,left,top,w,h,0xFFF0DEB8);
        g.drawString(font,nav.building()?"JOURNAL - CONTENTS":"GRANDMAS JOURNAL - CONTENTS",left+10,top+10,0x254D37,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        super.render(g,mx,my,delta);
        for(int r=0;r<rows&&page*rows+r<filtered.size();r++){
            var entry=filtered.get(page*rows+r);String detail=entry.section().title();
            if(progress)detail=JournalIndex.progress(snapshot,BreedingGuidePages.recipe(entry.page()));
            g.drawString(font,font.plainSubstrByWidth(detail,w-25),left+12,top+92+r*29,0x52614B,false);
            if(mx>=left+10&&mx<left+w-10&&my>=top+73+r*29&&my<top+102+r*29)
                g.renderTooltip(font,font.split(Component.literal(progress?detail+". Discovery is recorded in this book; it does not check your seed inventory.":entry.title()+" - "+entry.section().title()+". Click + to bookmark."),Math.min(280,w-20)),mx,my);
        }
        if(filtered.isEmpty())g.drawString(font,"No matching pages.",left+12,top+78,0x52614B,false);
        String status=marks.error().isEmpty()?(page+1)+" / "+Math.max(1,(filtered.size()+rows-1)/rows)+"  |  "+filtered.size()+" pages":marks.error();
        if(progress&&marks.error().isEmpty())status="Found "+IllustratedBreedingRecipes.ALL.stream().filter(r->snapshot.entry(r.result()).discovered()).count()+"/"+IllustratedBreedingRecipes.ALL.size()+" | Page "+(page+1)+"/"+Math.max(1,(filtered.size()+rows-1)/rows);
        g.drawString(font,status,left+72,top+h-20,0x254D37,false);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    private static final class GardenButton extends Button {
        GardenButton(int x,int y,int w,String text,Runnable action){super(x,y,w,18,Component.literal(text),b->action.run(),DEFAULT_NARRATION);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
            GardenGuiTheme.panel(g,getX(),getY(),width,height,isHoveredOrFocused()?0xFF397454:0xFF275841);
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,getMessage(),getX()+width/2,getY()+5,0xFFF0D0);
        }
    }
}
