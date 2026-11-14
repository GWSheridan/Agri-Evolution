package com.agrievolution.client.journal;

import java.util.*;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.client.GardenGuiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Read-only progression plan from gameplay recipes and this book's discoveries. */
public final class BreedingPlannerScreen extends Screen {
    private final Screen parent;
    private final JournalSnapshot snapshot;
    private CropSpecies target=CropSpecies.DIAMOND;
    private int left,top,w,h,rows,page;
    private String query="";
    private boolean remainingOnly;
    private BreedingPlanner.Step detail;
    private List<CropSpecies> targets=List.of(CropSpecies.values());
    private List<BreedingPlanner.Step> steps=List.of(),remaining=List.of();
    private List<FormattedCharSequence> detailLines=List.of();
    private Button view,next;
    public BreedingPlannerScreen(Screen parent,JournalSnapshot snapshot){super(Component.literal("Crop progression planner"));this.parent=parent;this.snapshot=snapshot;}
    private boolean known(CropSpecies species){return snapshot.entry(species).discovered();}
    @Override protected void init(){
        w=Math.min(460,width-16);h=Math.min(310,height-16);left=(width-w)/2;top=(height-h)/2;rows=Math.max(1,(h-156)/34);
        var search=new EditBox(font,left+10,top+26,w-20,18,Component.literal("Find target crop"));
        search.setMaxLength(100);search.setValue(query);search.setHint(Component.literal("Search target crop"));
        search.setResponder(value->{query=value;targets=Arrays.stream(CropSpecies.values()).filter(c->c.displayName().toLowerCase(Locale.ROOT).contains(value.strip().toLowerCase(Locale.ROOT))).toList();if(!targets.isEmpty()&&!targets.contains(target))target=targets.getFirst();detail=null;page=0;updatePlan();});addRenderableWidget(search);
        button("< Crop",left+10,top+49,58,()->cycle(-1));button("Crop >",left+w-68,top+49,58,()->cycle(1));
        view=button("Full chain",left+10,top+73,100,()->{if(detail!=null)detail=null;else remainingOnly=!remainingOnly;page=0;updatePlan();});
        next=button("Next needed",left+w-110,top+73,100,()->{if(!remaining.isEmpty()){detail=remaining.getFirst();page=0;updatePlan();}});
        button("<",left+10,top+h-24,24,()->page=Math.max(0,page-1));button(">",left+38,top+h-24,24,()->page=Math.min(pageCount()-1,page+1));
        button("Back",left+w-60,top+h-24,50,this::onClose);updatePlan();
    }
    private int detailRows(){return Math.max(1,(h-160)/11);}
    private int pageCount(){int size=detail==null?steps.size():detailLines.size(),per=detail==null?rows:detailRows();return Math.max(1,(size+per-1)/per);}
    private void updatePlan(){
        remaining=targets.isEmpty()?List.of():BreedingPlanner.remaining(target,this::known);
        steps=targets.isEmpty()?List.of():remainingOnly?remaining:BreedingPlanner.plan(target);
        detailLines=detail==null?List.of():detailText(detail).stream().flatMap(line->font.split(Component.literal(line),w-28).stream()).toList();
        page=Math.clamp(page,0,pageCount()-1);
        if(view!=null)view.setMessage(Component.literal(detail!=null?"Back to chain":remainingOnly?"Remaining only":"Full chain"));
        if(next!=null)next.active=!remaining.isEmpty();
    }
    private List<String> detailText(BreedingPlanner.Step step){
        var text=new ArrayList<String>();
        text.add(step.species().displayName()+" - "+(known(step.species())?"Discovered":"Undiscovered"));
        text.add("Method: "+BreedingPlanner.method(step));
        var recipe=step.recipe();
        if(recipe==null){text.add(BreedingPlanner.requirements(step));return text;}
        text.add("Parent: "+recipe.first().displayName()+" - "+(known(recipe.first())?"discovered":"undiscovered"));
        if(recipe.second()!=null)text.add("Parent: "+recipe.second().displayName()+" - "+(known(recipe.second())?"discovered":"undiscovered"));
        else text.add("Material: "+recipe.material().replace("minecraft:","").replace('_',' '));
        text.add("Base success: "+recipe.chance()+"%.");
        switch(recipe.method()){
            case CRAFT -> text.add("Combine the seed and material in a crafting grid.");
            case FIELD -> text.add("Sneak-use Grandmas Journal on one mature crop, then use it on the other within 8 blocks. Or use both seeds in a Breeding Station. Either parent order works.");
            case LAB -> text.add("Requires the Tier 3 Genetics Laboratory and one Nutrient Compost per attempt. Either parent order works.");
        }
        var missing=BreedingPlanner.missingParents(step,this::known);
        text.add(missing.isEmpty()?"Seed parents discovered; check you still have them.":"Undiscovered parents: "+String.join(", ",missing.stream().map(CropSpecies::displayName).toList()));
        if(recipe.method()!=IllustratedBreedingRecipes.Method.CRAFT)text.add("A failed cross returns a parent species. Supports and greenhouse bonuses can improve the base chance.");
        return text;
    }
    private void cycle(int delta){if(targets.isEmpty())return;target=targets.get(Math.floorMod(targets.indexOf(target)+delta,targets.size()));page=0;detail=null;updatePlan();}
    private Button button(String text,int x,int y,int width,Runnable action){return addRenderableWidget(new Button(x,y,width,18,Component.literal(text),b->action.run(),supplier->supplier.get()){
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){GardenGuiTheme.panel(g,getX(),getY(),getWidth(),getHeight(),isHoveredOrFocused()?0xFF397454:0xFF275841);g.drawCenteredString(font,getMessage(),getX()+getWidth()/2,getY()+5,0xFFF0DEB8);}
    });}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){super.renderBackground(g,mx,my,delta);GardenGuiTheme.panel(g,left,top,w,h,0xFFF0DEB8);}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        super.render(g,mx,my,delta);g.drawString(font,"GRANDMAS CROP PLANNER",left+10,top+10,0x254D37,false);
        String targetName=targets.isEmpty()?"No matching crop":target.displayName();
        g.drawCenteredString(font,font.plainSubstrByWidth(targetName,w-150),left+w/2,top+54,0x254D37);
        if(mx>=left+72&&mx<=left+w-72&&my>=top+49&&my<=top+68)g.renderTooltip(font,Component.literal(targetName),mx,my);
        if(detail!=null){
            g.drawString(font,"RECIPE DETAILS",left+12,top+98,0x254D37,false);
            for(int i=0;i<detailRows()&&page*detailRows()+i<detailLines.size();i++)g.drawString(font,detailLines.get(page*detailRows()+i),left+12,top+113+i*11,0x254D37,false);
        }else {
            String summary=targets.isEmpty()?"Try another crop name.":remaining.isEmpty()?"Target discovered in this journal.":remaining.size()+" needed | "+String.join(", ",remaining.stream().map(BreedingPlanner::method).distinct().map(m->m.replace("Field crossing / Breeding Station","Field / Station").replace("Tier 3 Genetics Laboratory","Tier 3 Lab")).toList());
            g.drawString(font,font.plainSubstrByWidth(summary,w-24),left+12,top+98,0x254D37,false);
            if(mx>=left+10&&mx<left+w-10&&my>=top+96&&my<top+110)g.renderTooltip(font,font.split(Component.literal(summary),w-24),mx,my);
            for(int i=0;i<rows&&page*rows+i<steps.size();i++){
                var step=steps.get(page*rows+i);int y=top+113+i*34;
                if(mx>=left+10&&mx<left+w-10&&my>=y&&my<y+32)g.fill(left+9,y,left+w-9,y+32,0x22397454);
                JournalRenderer.currentCrop(g,step.species(),left+12,y,24);
                String title=(page*rows+i+1)+". "+step.species().displayName()+" - "+(known(step.species())?"found":"needed");
                g.drawString(font,font.plainSubstrByWidth(title,w-55),left+42,y+1,known(step.species())?0x254D37:0x9D463E,false);
                g.drawString(font,font.plainSubstrByWidth(BreedingPlanner.method(step),w-55),left+42,y+13,0x52614B,false);
                if(mx>=left+10&&mx<left+w-10&&my>=y&&my<y+32)g.renderTooltip(font,font.split(Component.literal(BreedingPlanner.requirements(step)+". Click for parents and instructions."),Math.min(300,w-20)),mx,my);
            }
            if(steps.isEmpty()&&!targets.isEmpty())g.drawString(font,font.plainSubstrByWidth("No undiscovered steps. Use Full chain to review.",w-24),left+12,top+115,0x52614B,false);
        }
        g.drawString(font,font.plainSubstrByWidth("Discovery only; check your seed inventory.",w-20),left+10,top+h-39,0x52614B,false);
        g.drawString(font,"Page "+(page+1)+" / "+pageCount(),left+76,top+h-19,0x254D37,false);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(super.mouseClicked(x,y,button))return true;
        if(button==0&&detail==null&&x>=left+10&&x<left+w-10&&y>=top+113&&y<top+113+rows*34){int index=page*rows+(int)((y-top-113)/34);if(index<steps.size()){detail=steps.get(index);page=0;updatePlan();return true;}}
        return false;
    }
    @Override public void onClose(){if(detail!=null){detail=null;page=0;updatePlan();}else minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
