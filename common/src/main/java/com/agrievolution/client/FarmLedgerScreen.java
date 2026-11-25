package com.agrievolution.client;

import com.agrievolution.content.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import com.agrievolution.platform.Network;

/** Responsive native-size garden controls; plans remain drafts until explicitly saved. */
public final class FarmLedgerScreen extends AbstractContainerScreen<FarmLedgerMenu> {
    private static final String[] TABS = {"Fields", "Layouts", "Status", "Sharing"};
    private static final int INK=0xff342519, GREEN=0xff275841;
    private int tab, x, y, w, h, body, bodyH, slot, gx, gz, cols, rows, seedPage, listPage, statusPage, ticks;
    private int draftRevision;
    private UUID draftId;
    private UUID pendingField;
    private String[] draft;
    private String draftName="", search="", playerName="", fieldName="", selectedSeed="";
    private boolean dirty, editGuest, copying, confirmDelete, conflict;
    private boolean confirmDiscard;
    private String builtSearch="";
    private int sourceSlot, machineIndex;
    private EditBox nameBox, searchBox, playerBox;
    private List<Item> seedOptions = List.of();
    private String note="";
    private long noteUntil;
    public FarmLedgerScreen(FarmLedgerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Override protected void init() {
        imageWidth = Math.min(width-12,600); imageHeight=Math.min(height-12,366); super.init();
        x=leftPos; y=topPos; w=imageWidth; h=imageHeight; body=y+57; bodyH=h-101;
        seedOptions=BuiltInRegistries.ITEM.stream().filter(i->SeedBreedingParents.accepts(new ItemStack(i)))
                .sorted(Comparator.comparing(i->i.getDescription().getString())).toList();
        rebuild(); send("list", new CompoundTag());
    }
    private FarmLedgerData.Field field() { return FarmLedgerClient.selected; }
    private boolean owner() { return pendingField==null && field()!=null && minecraft.player!=null && field().owner.equals(minecraft.player.getUUID()); }
    private boolean editor() { return pendingField==null && field()!=null && minecraft.player!=null && field().canEdit(minecraft.player.getUUID()); }
    private void button(String text,int bx,int by,int bw,Runnable action,boolean active) {
        var b=addRenderableWidget(GardenGuiTheme.button(Component.literal(text),bx,by,bw,18,pressed->action.run())); b.active=active;
    }
    private EditBox box(int bx,int by,int bw,int max,String value,java.util.function.Consumer<String> change) {
        var box=new EditBox(font,bx,by,bw,16,Component.literal("Ledger text")); box.setMaxLength(max); box.setValue(value); box.setResponder(change); addRenderableWidget(box); return box;
    }
    private void rememberText() {
        if (tab==1 && !copying) { if(nameBox!=null)draftName=nameBox.getValue(); if(searchBox!=null)search=searchBox.getValue(); }
        if(tab==3 && playerBox!=null) playerName=playerBox.getValue();
    }
    private void rebuild() {
        labels.clear(); labelPositions.clear(); builtSearch=search;
        clearWidgets(); nameBox=null; searchBox=null; playerBox=null;
        for(int i=0;i<4;i++) {final int page=i; button(TABS[i],x+8+i*(w-16)/4,y+29,(w-16)/4-2,()->{rememberText();tab=page;copying=false;listPage=0;rebuild();},true);}
        if(tab==0 || copying) fields(); else if(field()==null) {
            button("Mark field",x+12,body+12,100,()->send("mark",new CompoundTag()),true);
            if(dirty)button("Discard draft",x+12,body+36,110,()->{dirty=false;conflict=false;draft=null;rebuild();},true);
        }
        else if(tab==1) layouts(); else if(tab==2) status(); else sharing();
        button(FarmLedgerClient.preview?"Preview: on":"Preview: off",x+8,y+h-42,96,()->{FarmLedgerClient.preview=!FarmLedgerClient.preview;FarmLedgerClient.layout=slot;send(FarmLedgerClient.preview?"watch":"stop",layoutTag());rebuild();},field()!=null);
        button("Done",x+w-62,y+h-42,54,this::onClose,true);
        if(tab==1&&!copying&&field()!=null){nameBox=box(x+110,y+h-41,w-180,32,draftName,s->{draftName=s;dirty=true;confirmDiscard=false;});nameBox.setEditable(editor());}
    }
    private void fields() {
        if(copying) {
            button("Source layout: "+(sourceSlot+1),x+8,body,130,()->{sourceSlot=(sourceSlot+1)%4;rebuild();},true);
            button("Cancel copy",x+144,body,90,()->{copying=false;rebuild();},true);
            button("<",x+w-66,body,24,()->page(-1),true);button(">",x+w-38,body,24,()->page(1),true);
        } else {
            button("Mark field",x+8,body,84,()->send("mark",new CompoundTag()),true);
            button("<",x+w-74,body,28,()->page(-1),true); button(">",x+w-40,body,28,()->page(1),true);
            nameBox=box(x+8,body+23,w-164,32,fieldName,s->fieldName=s);
            button("Rename",x+w-150,body+22,66,()->{var t=new CompoundTag();t.putString("Name",nameBox.getValue());send("rename",t);},owner());
            button(confirmDelete?"Confirm?":"Delete",x+w-78,body+22,66,()->{if(confirmDelete){send("delete",new CompoundTag());confirmDelete=false;}else{confirmDelete=true;rebuild();}},owner());
        }
        var list=FarmLedgerClient.latest.getList("Fields",Tag.TAG_COMPOUND); int startY=body+(copying?24:46);
        int count=Math.max(1,(bodyH-(copying?25:47))/22); int start=listPage*count;
        for(int j=start;j<Math.min(list.size(),start+count);j++) {
            var t=list.getCompound(j); if(!t.hasUUID("Id"))continue;
            String label=t.getString("Name")+"  "+t.getInt("Width")+"×"+t.getInt("Depth"); UUID id=t.getUUID("Id");
            button(font.plainSubstrByWidth(label,w-35),x+8,startY+(j-start)*22,w-16,()->{
                if(copying) {var input=layoutTag();input.putUUID("Source",id);input.putInt("SourceLayout",sourceSlot);send("copy",input);copying=false;}
                else if(dirty && !id.equals(draftId)) {note("Save or reload your draft before selecting another field.");return;}
                else {pendingField=id;sendTo("select",id,t.getInt("Revision"),layoutTag());confirmDelete=false;}
                rebuild();
            }, !copying || field()!=null && t.getInt("Width")==field().width && t.getInt("Depth")==field().depth);
        }
    }
    private void page(int delta) {
        int count=Math.max(1,(bodyH-(copying?25:47))/22);int size=FarmLedgerClient.latest.getList("Fields",Tag.TAG_COMPOUND).size();
        int next=listPage+delta;if(next>=0&&next<Math.max(1,(size+count-1)/count)){listPage=next;rebuild();return;}
        var t=new CompoundTag();t.putInt("Page",Math.clamp(FarmLedgerClient.latest.getInt("Page")+delta,0,Math.max(0,FarmLedgerClient.latest.getInt("Pages")-1)));listPage=0;send("list",t);
    }
    private void layouts() {
        var f=field(); if(draft==null || !f.id.equals(draftId)) loadDraft();
        for(int i=0;i<4;i++){final int n=i;button((slot==i?"[":"")+(i+1)+(slot==i?"]":""),x+8+i*28,body,26,()->{
            if(dirty){note("Save or reload your draft before switching layouts.");return;} rememberText();slot=n;loadDraft();FarmLedgerClient.layout=slot;send("select",layoutTag());rebuild();},true);}
        button("Save",x+124,body,48,()->{rememberText();var t=layoutTag();t.putString("Name",draftName);t.put("Plan",FarmLedgerData.strings(draft));sendTo("save",draftId,draftRevision,t);},editor()&&!conflict);
        button("Reload",x+176,body,52,()->{loadDraft();send("select",layoutTag());rebuild();},true);
        button("Copy",x+232,body,48,()->{copying=true;listPage=0;rebuild();},editor()&&!dirty);
        int right=x+w-136; cols=Math.min(f.width,Math.max(1,(right-x-18)/16));rows=Math.min(f.depth,Math.max(1,(bodyH-40)/16));
        gx=Math.clamp(gx,0,Math.max(0,f.width-cols));gz=Math.clamp(gz,0,Math.max(0,f.depth-rows));
        searchBox=box(right,body+27,126,80,search,s->{search=s;seedPage=0;});searchBox.setHint(Component.literal("Search seeds"));
        int seedRows=Math.max(1,(bodyH-68)/18); var options=filtered(); int offset=seedPage*seedRows;
        for(int j=0;j<seedRows && offset+j<options.size();j++) {
            String id=options.get(offset+j);String label=id.isEmpty()?"Clear cell":seedName(id);
            button(font.plainSubstrByWidth(label,114),right,body+48+j*18,126,()->{selectedSeed=id;note(id.isEmpty()?"Clear selected cells.":"Selected "+seedName(id));},editor());
        }
        button("<",right,body+bodyH-22,24,()->{rememberText();seedPage=Math.max(0,seedPage-1);rebuild();},true);
        button(">",right+27,body+bodyH-22,24,()->{rememberText();seedPage=Math.min(Math.max(0,(filtered().size()-1)/seedRows),seedPage+1);rebuild();},true);
        button(hasShiftDown()?"Shift: row":"Paint cells",right+56,body+bodyH-22,70,()->note("Click to paint; Shift-click paints the whole row. Scroll to move the grid."),true);
    }
    private List<String> filtered() {
        var result=new ArrayList<String>();result.add("");String term=search.toLowerCase(Locale.ROOT);
        for(var item:seedOptions){String id=BuiltInRegistries.ITEM.getKey(item).toString();if(id.contains(term)||item.getDescription().getString().toLowerCase(Locale.ROOT).contains(term))result.add(id);}return result;
    }
    private void status() {
        int line=body+2;var f=field();int mature=0,weeds=0,dry=0,food=0,support=0,unknown=0,rotation=0,mulched=0;
        for(int flag:f.flags){if((flag&1)==0)unknown++;if((flag&8)!=0)mature++;if((flag&16)!=0)weeds++;if((flag&32)!=0)dry++;if((flag&64)!=0)food++;if((flag&128)!=0)support++;}
        for(int flag:f.flags){if((flag&256)!=0)rotation++;if((flag&512)!=0)mulched++;}
        String when=f.surveyedAt==0?"Not surveyed":DateTimeFormatter.ofPattern("dd MMM HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(f.surveyedAt));
        label("Mature "+mature+" | weeds "+weeds+" | dry "+dry, x+10,line);line+=12;
        label("Low nutrition "+food+" | supports "+support+" | unknown "+unknown,x+10,line);line+=12;
        label("Rotation bonus "+rotation+" | mulched plots "+mulched,x+10,line);line+=12;
        label((FarmLedgerClient.latest.getBoolean("Nearby")?"Nearby survey":"Saved snapshot")+": "+when,x+10,line);line+=16;
        var needs=new TreeMap<String,int[]>();for(int i=0;i<f.size();i++){String seed=f.layouts[slot].seeds[i];if(seed.isEmpty())continue;String state=FarmLedgerService.cellStatus(f,slot,i);var a=needs.computeIfAbsent(seed,k->new int[2]);if(state.equals("Unknown"))a[1]++;else if(!state.equals("Correct"))a[0]++;}
        var details=new ArrayList<String>();var carried=FarmLedgerClient.latest.getCompound("Carried");
        needs.forEach((id,a)->details.add(seedName(id)+": need "+a[0]+", carried "+carried.getInt(id)+(a[1]>0?", unknown "+a[1]:"")));
        details.addAll(f.machineStatus);if(details.isEmpty())details.add("Assign a layout to see seed requirements.");
        int count=Math.max(1,(bodyH-80)/13);statusPage=Math.min(statusPage,Math.max(0,(details.size()-1)/count));
        for(int i=statusPage*count;i<Math.min(details.size(),(statusPage+1)*count);i++){label(details.get(i),x+10,line);line+=13;}
        button("More",x+w-62,body,54,()->{statusPage=(statusPage+1)%Math.max(1,(details.size()+count-1)/count);rebuild();},true);
        if(!f.machines.isEmpty()){
            machineIndex=Math.min(machineIndex,f.machines.size()-1);
            button("Link "+(machineIndex+1)+"/"+f.machines.size(),x+8,body+bodyH-20,76,()->{machineIndex=(machineIndex+1)%f.machines.size();rebuild();},true);
            button("Unlink selected",x+90,body+bodyH-20,116,()->{var t=new CompoundTag();t.putInt("Index",machineIndex);send("unlink",t);},editor());
        }
    }
    private final List<String> labels=new ArrayList<>();
    private final List<int[]> labelPositions=new ArrayList<>();
    private void label(String s,int lx,int ly){labels.add(s);labelPositions.add(new int[]{lx,ly});}
    private void sharing() {
        playerBox=box(x+10,body+2,w-126,36,playerName,s->playerName=s);
        playerBox.setHint(Component.literal("Player name / UUID"));
        button(editGuest?"Can edit":"View only",x+w-108,body,98,()->{rememberText();editGuest=!editGuest;rebuild();},owner());
        button("Invite player",x+10,body+23,100,()->{rememberText();var t=new CompoundTag();t.putString("Player",playerName);t.putBoolean("Edit",editGuest);send("invite",t);},owner());
        int count=Math.max(1,(bodyH-48)/22);var invited=new ArrayList<>(field().invited.entrySet());int pages=Math.max(1,(invited.size()+count-1)/count);listPage=Math.min(listPage,pages-1);
        button("More",x+w-62,body+23,54,()->{rememberText();listPage=(listPage+1)%pages;rebuild();},true);
        for(int i=listPage*count;i<Math.min(invited.size(),(listPage+1)*count);i++){
            var entry=invited.get(i);var info=minecraft.getConnection()==null?null:minecraft.getConnection().getPlayerInfo(entry.getKey());
            String name=info==null?entry.getKey().toString():info.getProfile().getName();int ly=body+48+(i-listPage*count)*22;
            label(name+(entry.getValue()?" (edit)":" (view)"),x+10,ly+4);
            button("Revoke",x+w-68,ly,58,()->{var t=new CompoundTag();t.putString("Player",entry.getKey().toString());send("revoke",t);},owner());
        }
    }
    private void loadDraft(){var f=field();if(f==null)return;draftId=f.id;draftRevision=f.revision;draft=f.layouts[slot].seeds.clone();draftName=f.layouts[slot].name;dirty=false;conflict=false;}
    public void receive(){
        rememberText();
        String message=FarmLedgerClient.latest.getString("Message");if(!message.isEmpty())note(message);
        boolean selectionChanged=field()==null||draftId==null||!field().id.equals(draftId);
        if(field()==null||field().id.equals(pendingField))pendingField=null;
        var f=field();if(f!=null){if(tab!=0||!hasFocusedEditBox())fieldName=f.name;if(!dirty)loadDraft();else if(f.id.equals(draftId)&&f.revision!=draftRevision){
            if(Arrays.equals(f.layouts[slot].seeds,draft)&&f.layouts[slot].name.equals(draftName)){dirty=false;loadDraft();}else conflict=true;
        }}
        // Leave focused text and unsaved edits intact during periodic survey replies.
        if(selectionChanged||!hasFocusedEditBox()) rebuild();
    }
    private boolean hasFocusedEditBox(){return getFocused() instanceof EditBox;}
    private CompoundTag layoutTag(){var t=new CompoundTag();t.putInt("Layout",slot);t.putBoolean("Preview",FarmLedgerClient.preview);return t;}
    private void send(String action,CompoundTag t){var f=field();sendTo(action,f==null?FarmLedgerData.NONE:f.id,f==null?0:f.revision,t);}
    private void sendTo(String action,UUID id,int revision,CompoundTag t){if(minecraft.getConnection()!=null)Network.sendToServer(new FarmLedgerPayload.Request(menu.containerId,action,id,revision,t));}
    private void note(String value){note=value;noteUntil=System.nanoTime()+6_000_000_000L;}
    @Override public void onClose(){if(dirty&&!confirmDiscard){confirmDiscard=true;note("Unsaved draft. Save or Reload, or close again to discard it.");return;}super.onClose();}
    @Override protected void containerTick(){super.containerTick();if(++ticks%20==0&&field()!=null)send("watch",layoutTag());
        if(tab==1&&searchBox!=null&&!builtSearch.equals(search)){int cursor=searchBox.getCursorPosition();rememberText();rebuild();setFocused(searchBox);searchBox.setFocused(true);searchBox.moveCursorTo(cursor,false);}}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) { }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        GardenGuiTheme.panel(g,x,y,w,h,0xff281c17);GardenGuiTheme.panel(g,x+6,y+5,w-12,20,GREEN);
        g.drawString(font,"AGRI EVOLUTION / FARM LEDGER",x+12,y+11,0xfff0dfbb,false);
        GardenGuiTheme.panel(g,x+6,body-4,w-12,bodyH+4,0xfff0dfbb);
        for(int i=0;i<labels.size();i++){var p=labelPositions.get(i);g.drawString(font,font.plainSubstrByWidth(labels.get(i),w-(tab==3?94:24)),p[0],p[1],INK,false);}
        if(tab==1&&!copying&&field()!=null&&draft!=null){
            var f=field();for(int rz=0;rz<rows;rz++)for(int cx=0;cx<cols;cx++){
                int index=(rz+gz)*f.width+cx+gx, px=x+9+cx*16,py=body+28+rz*16;
                String state=FarmLedgerService.cellStatus(f,slot,index);int color=color(state);
                g.fill(px,py,px+15,py+15,color);String seed=draft[index];var id=ResourceLocation.tryParse(seed);
                if(id!=null&&BuiltInRegistries.ITEM.containsKey(id))g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(id)),px,py);
                if(!seed.equals(f.layouts[slot].seeds[index]))g.fill(px,py,px+3,py+3,0xffffffff);
            }
            g.drawString(font,"Grid "+(gx+1)+","+(gz+1)+" • scroll / Shift-scroll",x+10,body+bodyH-10,INK,false);
        }
        String footer=System.nanoTime()<noteUntil?note:conflict?"Field changed. Draft kept; Reload before saving.":dirty?"Unsaved layout — Save or Reload.":field()==null?"Mark a field to begin.":field().name+" • "+field().dimension+" • "+field().min.toShortString();
        g.drawString(font,font.plainSubstrByWidth(footer,w-16),x+8,y+h-18,0xfff0dfbb,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        super.render(g,mx,my,partial);
        if(tab==1&&!copying&&field()!=null&&draft!=null){int cell=gridCell(mx,my);if(cell>=0)g.renderTooltip(font,Component.literal(seedName(draft[cell])+" — "+FarmLedgerService.cellStatus(field(),slot,cell)),mx,my);}
    }
    private int gridCell(double mx,double my){int cx=(int)Math.floor((mx-x-9)/16),rz=(int)Math.floor((my-body-28)/16);return cx>=0&&rz>=0&&cx<cols&&rz<rows?(rz+gz)*field().width+cx+gx:-1;}
    private boolean paint(double mx,double my){if(tab!=1||copying||field()==null||draft==null||!editor())return false;int cell=gridCell(mx,my);if(cell<0)return false;
        if(hasShiftDown()){int start=cell/field().width*field().width;Arrays.fill(draft,start,start+field().width,selectedSeed);}else draft[cell]=selectedSeed;dirty=true;confirmDiscard=false;return true;}
    @Override public boolean mouseClicked(double mx,double my,int button){if(button==0&&paint(mx,my))return true;return super.mouseClicked(mx,my,button);}
    @Override public boolean mouseDragged(double mx,double my,int b,double dx,double dy){if(b==0&&paint(mx,my))return true;return super.mouseDragged(mx,my,b,dx,dy);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(tab==1&&!copying&&field()!=null){if(hasShiftDown())gx=Math.clamp(gx-(int)vertical,0,Math.max(0,field().width-cols));else gz=Math.clamp(gz-(int)vertical,0,Math.max(0,field().depth-rows));return true;}return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    public static int color(String status){return switch(status){case "Correct"->0xff53955c;case "Missing"->0xffccac46;case "Wrong plant"->0xffbb5351;case "Unsuitable soil"->0xff9261b2;case "Unknown"->0xff888888;default->0xffb7a382;};}
    private String seedName(String id){if(id.isEmpty())return "Unassigned";var key=ResourceLocation.tryParse(id);return key!=null&&BuiltInRegistries.ITEM.containsKey(key)?BuiltInRegistries.ITEM.get(key).getDescription().getString():"Missing: "+id;}
}
