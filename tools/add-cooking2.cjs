const fs=require('fs'),meals=require('./cooking2-spec.json');
const j='src/main/java/com/agrievolution/',res='src/main/resources/',a=res+'assets/agri_evolution/';
const changes=new Map();
const edit=(p,f)=>{const old=changes.get(p)??fs.readFileSync(p,'utf8'),s=f(old);if(s===old)throw Error('No edit '+p);changes.set(p,s);};
const write=(p,v)=>changes.set(p,JSON.stringify(v,null,2)+'\n');
const all=require('./cooking-spec.json');
for(const m of meals){if(all.some(x=>x.id===m.id))throw Error('Dish already exists');if(fs.existsSync(res+'data/agri_evolution/recipe/'+m.id+'.json'))throw Error('Recipe already exists');}
edit(j+'content/CookingFoods.java',s=>s.replace('\n    );',',\n'+meals.map(m=>'        dish('+JSON.stringify(m.id)+','+m.nutrition+','+m.saturation+'f,'+m.bowl+',new String[]{'+m.ingredients.map(JSON.stringify).join(',')+'})').join(',\n')+'\n    );'));
write('tools/cooking-spec.json',all.concat(meals));
for(const m of meals){
 write(a+'models/item/'+m.id+'.json',{parent:'minecraft:item/generated',textures:{layer0:'agri_evolution:item/'+m.id}});
 write(res+'data/agri_evolution/recipe/'+m.id+'.json',{type:'minecraft:crafting_shapeless',category:'food',ingredients:m.ingredients.map(item=>({item})),result:{id:'agri_evolution:'+m.id,count:1}});
 write(res+'data/agri_evolution/advancement/recipes/food/'+m.id+'.json',{parent:'minecraft:recipes/root',criteria:{has_ingredient:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:m.ingredients.find(i=>i.startsWith('agri_evolution:'))}]}},has_recipe:{trigger:'minecraft:recipe_unlocked',conditions:{recipe:'agri_evolution:'+m.id}}},requirements:[['has_ingredient','has_recipe']],rewards:{recipes:['agri_evolution:'+m.id]}});
}
edit(a+'lang/en_us.json',s=>{const o=JSON.parse(s);for(const m of meals)o['item.agri_evolution.'+m.id]=m.name;return JSON.stringify(o,null,2)+'\n';});
const pages=[
 ['GARDEN LUNCHES','MORE HOMEMADE FOOD',['Try Salmon & Dill','Sandwich, Pulled Pork','Slaw Roll or Chickpea','Salad Wrap. Craft using','cooked meat or fish.','Each makes one serving.'],['For a hearty bowl, try','Mutton & Barley Soup,','Creamy Cauliflower Soup','or Spicy Lentil Soup.','Eating returns the bowl.','Milk returns its bucket.']],
 ['SWEET AND SAVOURY','GARDEN KITCHEN II',['Try Rhubarb Custard Tart,','Grape & Oat Cookie or','Melon & Berry Sundae.','Desserts restore hunger','but have less saturation','than a hearty main meal.'],['Try Beef & Garden','Vegetable Pie, Garlic Cod','Dinner or Sesame Chicken','Rice Bowl. Browse their','pictured recipes here,','or use Contents / Search.']]
];
edit(j+'client/journal/MachineGuidePages.java',s=>s.replace('32 HOMEMADE DISHES','44 HOMEMADE DISHES').replace('private static final Page[] PAGES = {','private static final Page[] PAGES = {\n'+pages.map(([t,sub,l,r])=>'        new Page('+JSON.stringify(t)+','+JSON.stringify(sub)+',new String[]{'+l.map(JSON.stringify).join(',')+'},new String[]{'+r.map(JSON.stringify).join(',')+'}),').join('\n')).replace('PLANT_PAGES=java.util.Set.of(','PLANT_PAGES=java.util.Set.of('+pages.map(p=>JSON.stringify(p[0])).join(',')+','));
edit('src/test/java/com/agrievolution/content/CookingGameTests.java',s=>s.replace('ALL.size()==32','ALL.size()==44').replace('32 dishes loaded','44 dishes loaded'));
edit('src/guiPreview/java/com/agrievolution/preview/GrandmasJournalPreview.java',s=>s.replace('?8:64','?24:88').replace('"food6-cooking-"','"cooking2-"').replace('"braised_garden_greens","beef_pulse_stew","chickpea_grain_bowl","summer_fruit_compote"',meals.map(m=>JSON.stringify(m.id)).join(',')));
edit('gradle.properties',s=>s.replace('mod_version=0.64.0','mod_version=0.65.0'));
for(const [p,s]of changes)fs.writeFileSync(p,s);
console.log('12 dishes added, 44 total; previous recipes preserved.');
