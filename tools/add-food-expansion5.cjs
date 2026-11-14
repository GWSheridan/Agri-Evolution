const fs=require('fs'),path=require('path');
const {names,field}=require('./food5-spec.json');
const java='src/main/java/com/agrievolution/',res='src/main/resources/',assets=res+'assets/agri_evolution/';
const up=n=>n.toUpperCase(),title=n=>n.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' ');
if(fs.readFileSync(java+'genetics/CropSpecies.java','utf8').includes('SPINACH('))throw Error('Already applied');
const edit=(p,f)=>{const old=fs.readFileSync(p,'utf8'),next=f(old);if(next===old)throw Error('No edit: '+p);fs.writeFileSync(p,next);};
const write=(p,v)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');};
edit(java+'genetics/CropSpecies.java',s=>s.replace('"cranberry_seed", true);','"cranberry_seed", true),\n'+names.map(n=>'    '+up(n)+'("'+n+'", "'+title(n)+'", "'+n+'_crop", "'+n+'_seed", true)').join(',\n')+';').replace('ordinal() <= CRANBERRY.ordinal()', 'ordinal() <= BLACKCURRANT.ordinal()'));
edit(java+'content/AEItems.java',s=>s.replace('    public static final DeferredItem<Item> BREEDING_JOURNAL',names.map((n,i)=>'    public static final DeferredItem<Item> '+up(n)+' = ITEMS.registerItem("'+n+'", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition('+(i<5?3:2)+').saturationModifier(0.3f).build()));\n    public static final DeferredItem<Item> '+up(n)+'_SEED = ITEMS.registerItem("'+n+'_seed", props -> new SpeciesSeedItem(props, CropSpecies.'+up(n)+', AEBlocks.'+up(n)+'_CROP::get), new Item.Properties());').join('\n')+'\n    public static final DeferredItem<Item> BREEDING_JOURNAL').replace('case TURMERIC -> TURMERIC_SEED.get();','case TURMERIC -> TURMERIC_SEED.get();\n'+names.map(n=>'            case '+up(n)+' -> '+up(n)+'_SEED.get();').join('\n')));
edit(java+'content/AEBlocks.java',s=>s.replace('    public static final DeferredBlock<AgriCropBlock> NETHERITE_CROP',names.map(n=>'    public static final DeferredBlock<AgriCropBlock> '+up(n)+'_CROP = BLOCKS.register("'+n+'_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.'+up(n)+', AEItems.'+up(n)+'_SEED::get));').join('\n')+'\n    public static final DeferredBlock<AgriCropBlock> NETHERITE_CROP'));
edit(java+'content/AEBlockEntities.java',s=>s.replace('AEBlocks.CRANBERRY_CROP.get()).build','AEBlocks.CRANBERRY_CROP.get(), '+names.map(n=>'AEBlocks.'+up(n)+'_CROP.get()').join(', ')+').build'));
edit(java+'content/AECreativeTabs.java',s=>s.replace('output.accept(AEItems.BREEDING_JOURNAL.get());',names.map(n=>'output.accept(AEItems.'+up(n)+'_SEED.get()); output.accept(AEItems.'+up(n)+'.get());').join('\n                        ')+'\n                        output.accept(AEItems.BREEDING_JOURNAL.get());'));
for(const file of ['content/AgriCropBlock.java','content/BotanicalGreenhouseBlockEntity.java'])edit(java+file,s=>s.replaceAll('case TURMERIC -> AEItems.TURMERIC.get();','case TURMERIC -> AEItems.TURMERIC.get();\n'+names.map(n=>'            case '+up(n)+' -> AEItems.'+up(n)+'.get();').join('\n')).replace('case TURMERIC -> AEBlocks.TURMERIC_CROP.get();','case TURMERIC -> AEBlocks.TURMERIC_CROP.get();\n'+names.map(n=>'                    case '+up(n)+' -> AEBlocks.'+up(n)+'_CROP.get();').join('\n')));
for(const [file,v]of [['CompactJournalRenderer.java','plant'],['JournalScreen.java','species']])edit(java+'client/journal/'+file,s=>s.replace('case BASIL, MINT, ROSEMARY, THYME, PARSLEY, CORIANDER, GINGER, TURMERIC, PUMPKIN, BUTTERNUT_SQUASH, COURGETTE, TURNIP, PARSNIP, LEEK, CELERY, ASPARAGUS, BLUEBERRY, RASPBERRY, BLACKBERRY, CRANBERRY -> '+v+'.displayName();','case BASIL, MINT, ROSEMARY, THYME, PARSLEY, CORIANDER, GINGER, TURMERIC, PUMPKIN, BUTTERNUT_SQUASH, COURGETTE, TURNIP, PARSNIP, LEEK, CELERY, ASPARAGUS, BLUEBERRY, RASPBERRY, BLACKBERRY, CRANBERRY, '+names.map(up).join(', ')+' -> '+v+'.displayName();'));
edit(java+'genetics/SpeciesBreeder.java',s=>s.replace('        return null;',field.map(([a,b,c,p])=>'        if (pair(first, second, CropSpecies.'+a+', CropSpecies.'+b+'))\n            return new MutationRecipe(CropSpecies.'+c+', '+p+');').join('\n')+'\n        return null;'));
for(const n of names){
 for(const p of ['blockstates/tomato_crop.json',...Array.from({length:8},(_,i)=>'models/block/tomato_crop_stage'+i+'.json'),'models/item/tomato_seed.json','models/item/tomato.json']){
 fs.writeFileSync(assets+p.replaceAll('tomato',n),fs.readFileSync(assets+p,'utf8').replaceAll('tomato',n));
 }
 write(res+'data/agri_evolution/loot_table/blocks/'+n+'_crop.json',JSON.parse(fs.readFileSync(res+'data/agri_evolution/loot_table/blocks/tomato_crop.json','utf8').replaceAll('tomato',n)));
}
edit(assets+'lang/en_us.json',s=>{const j=JSON.parse(s);for(const n of names){j['block.agri_evolution.'+n+'_crop']=title(n)+' Crop';j['item.agri_evolution.'+n]=title(n);j['item.agri_evolution.'+n+'_seed']=title(n)+' Seeds';}return JSON.stringify(j,null,2)+'\n';});
edit('gradle.properties',s=>s.replace('mod_version=0.62.0','mod_version=0.63.0'));
const t='src/test/java/com/agrievolution/';
edit(t+'genetics/SpeciesSmokeTest.java',s=>s.replace('length != 203','length != 215').replace('public static void main(String[] args) {','public static void main(String[] args) {\n'+field.map(([a,b,c,p])=>'        verifyRecipe(CropSpecies.'+a+',CropSpecies.'+b+',CropSpecies.'+c+','+p+');').join('\n')));

edit(t+'genetics/ResourceProgressionSelfTest.java',s=>s.replace('count()!=36','count()!=48'));
edit(t+'content/FoodCropGameTests.java',s=>s.replace('index==36','index==48').replace('Thirty-six food crops','Forty-eight food crops'));
edit(t+'content/CookingGameTests.java',s=>s.replace('covered==36','covered==48').replace('All 36 food crops','All 48 food crops').replace('ALL.size()==24','ALL.size()==28').replace('24 dishes','28 dishes'));
edit(t+'client/journal/JournalNavigationSelfTest.java',s=>s.replaceAll('CropSpecies.CRANBERRY','CropSpecies.BLACKCURRANT').replace('== 51','== 54').replace('fifty-one species','fifty-four species').replaceAll('==200','==212').replaceAll('<=200','<=212').replace('all 198 crosses','all 210 crosses').replace('==136','==148').replace('all 203 breeding','all 215 breeding'));
edit('src/guiPreview/java/com/agrievolution/preview/GrandmasJournalPreview.java',s=>s.replace('CropSpecies.PUMPKIN.ordinal()','CropSpecies.SPINACH.ordinal()').replace('?48:', '?56:'));
console.log('12 new food crops appended; existing ordinals retained.');


