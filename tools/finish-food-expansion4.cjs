const fs=require('fs'),{names}=require('./food4-spec.json');
const root='src/main/resources/assets/agri_evolution/';
for(const n of names)for(let i=0;i<10;i++){const file=n+(i<8?'_crop_stage'+i:i===8?'':'_seed')+'.png';fs.copyFileSync('artwork/food-crops-v4/sprites/'+file,root+'textures/'+(i<8?'block/':'item/')+file);}
const pages=[
['SQUASH AND ROOTS','FOOD GARDEN IV',['Cucumber + Corn:','Pumpkin (24%).','Pumpkin + Sweet Potato:','Butternut Squash (22%).','Pumpkin + Cucumber:','Courgette (24%).'],['Radish + Cabbage:','Turnip (26%).','Turnip + Parsley:','Parsnip (24%).','Mature plants yield food.','Grow on ordinary farmland.']],
['STALKS AND SPEARS','FOOD GARDEN IV',['Parsley + Lettuce:','Celery (26%).','Onion + Celery:','Leek (24%).','Leek + Broccoli:','Asparagus (22%).'],['Eight stages per crop.','Wait until stage 7.','All harvests are edible.','Use Food / Flowers in','greenhouse planting.','Keep soil hydrated.']],
['BERRY PATCH','FOOD GARDEN IV',['Strawberry + Azure Flax:','Blueberry (22%).','Strawberry + Crimson','Clover: Raspberry (24%).','Raspberry + Blueberry:','Blackberry (20%).'],['Blueberry + Rice:','Cranberry (22%).','All are field crosses.','Supports improve chances.','Use illustrated recipes','to plan each parent.']],
['FOOD GARDEN IV TIPS','GROWING AND HARVESTING',['These crops stay in one','farmland block, including','the pumpkin and squash.','No nearby fruit space','is required. Crops use','the centred X model.'],['Pumpkin here is edible','Agri Evolution produce.','Vanilla pumpkins and','their recipes are separate.','Replant genetic seeds','or use your greenhouse.']]
];
const p='src/main/java/com/agrievolution/client/journal/MachineGuidePages.java';let s=fs.readFileSync(p,'utf8');if(s.includes('"SQUASH AND ROOTS"'))throw Error('Already applied');
s=s.replace('private static final Page[] PAGES = {','private static final Page[] PAGES = {\n'+pages.map(([t,sub,l,r])=>'        new Page('+JSON.stringify(t)+','+JSON.stringify(sub)+',new String[]{'+l.map(JSON.stringify).join(',')+'},new String[]{'+r.map(JSON.stringify).join(',')+'}),').join('\n'));
s=s.replace('PLANT_PAGES=java.util.Set.of(','PLANT_PAGES=java.util.Set.of('+pages.map(p=>JSON.stringify(p[0])).join(',')+',');fs.writeFileSync(p,s);
console.log('120 original sprites installed; four crop-only journal guides added.');

