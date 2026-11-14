const fs=require('fs'),{names,field}=require('./food6-spec.json');
const j='src/main/java/com/agrievolution/',res='src/main/resources/',a=res+'assets/agri_evolution/';
const meals=require('./food6-meals.json');
const edit=(p,f)=>{let s=fs.readFileSync(p,'utf8'),n=f(s);if(n===s)throw Error('No change '+p);fs.writeFileSync(p,n);};
if(fs.readFileSync(j+'content/CookingFoods.java','utf8').includes('braised_garden_greens'))throw Error('Already applied');
edit(j+'content/CookingFoods.java',s=>s.replace('\n    );',',\n'+meals.map(m=>'        dish('+JSON.stringify(m.id)+','+m.nutrition+','+m.saturation+'f,'+m.bowl+',new String[]{'+m.ingredients.map(JSON.stringify).join(',')+'})').join(',\n')+'\n    );'));
const all=require('./cooking-spec.json');fs.writeFileSync('tools/cooking-spec.json',JSON.stringify(all.concat(meals),null,2)+'\n');
for(const m of meals){
 const write=(p,v)=>fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');
 write(a+'models/item/'+m.id+'.json',{parent:'minecraft:item/generated',textures:{layer0:'agri_evolution:item/'+m.id}});
 write(res+'data/agri_evolution/recipe/'+m.id+'.json',{type:'minecraft:crafting_shapeless',category:'food',ingredients:m.ingredients.map(item=>({item})),result:{id:'agri_evolution:'+m.id,count:1}});
 write(res+'data/agri_evolution/advancement/recipes/food/'+m.id+'.json',{parent:'minecraft:recipes/root',criteria:{has_ingredient:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:m.ingredients.find(i=>i.startsWith('agri_evolution:'))}]}},has_recipe:{trigger:'minecraft:recipe_unlocked',conditions:{recipe:'agri_evolution:'+m.id}}},requirements:[['has_ingredient','has_recipe']],rewards:{recipes:['agri_evolution:'+m.id]}});
}
edit(a+'lang/en_us.json',s=>{const o=JSON.parse(s);for(const m of meals)o['item.agri_evolution.'+m.id]=m.name;return JSON.stringify(o,null,2)+'\n';});
const pages=[["GREENS AND PULSES","FOOD GARDEN VI",["Spinach + Cabbage:","Kale (26%).","Kale + Lettuce:","Pak Choi (24%).","Cabbage + Cauliflower:","Brussels Sprouts (22%)."],["Turnip + Sweet Potato:","Swede (24%).","Celery + Turnip:","Celeriac (24%).","Peas + Sesame:","Lentil (24%)."]],["GRAINS AND FRUIT","FOOD GARDEN VI",["Beans + Peanut:","Chickpea (24%).","Wild Wheat + Oats:","Barley (26%).","Barley + Amber Marigold:","Buckwheat (22%)."],["Celery + Cranberry:","Rhubarb (22%).","Blackcurrant + Cucumber:","Grape (22%).","Pumpkin + Gooseberry:","Cantaloupe (22%)."]],["GARDEN KITCHEN VI","NEW HARVEST MEALS",["All twelve crops grow","on ordinary farmland.","Wait until stage 7.","Each plant uses one cell.","Greenhouses can harvest","and replant every species."],["Try Braised Garden Greens,","Beef & Lentil Stew,","Chickpea Grain Bowl or","Summer Fruit Compote.","Recipes are pictured here.","Bowl meals return a bowl."]]];
edit(j+'client/journal/MachineGuidePages.java',s=>s.replace('28 HOMEMADE DISHES','32 HOMEMADE DISHES').replace('private static final Page[] PAGES = {','private static final Page[] PAGES = {\n'+pages.map(([t,sub,l,r])=>'        new Page('+JSON.stringify(t)+','+JSON.stringify(sub)+',new String[]{'+l.map(JSON.stringify).join(',')+'},new String[]{'+r.map(JSON.stringify).join(',')+'}),').join('\n')).replace('PLANT_PAGES=java.util.Set.of(','PLANT_PAGES=java.util.Set.of('+pages.map(p=>JSON.stringify(p[0])).join(',')+','));
console.log('Four new dishes and crop-only guide pages added.');
