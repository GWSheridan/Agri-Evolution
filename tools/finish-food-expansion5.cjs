const fs=require('fs'),{names,field}=require('./food5-spec.json');
const j='src/main/java/com/agrievolution/',res='src/main/resources/',a=res+'assets/agri_evolution/';
const meals=[
 {id:'green_garden_broth',name:'Green Garden Broth',category:'soup',nutrition:10,saturation:0.65,bowl:true,ingredients:['minecraft:bowl','agri_evolution:spinach','agri_evolution:spring_onion','agri_evolution:dill'],visual:'rustic wooden bowl of green spinach broth with pale spring onion slices and dill'},
 {id:'roasted_vegetable_bowl',name:'Roasted Vegetable Bowl',category:'meal',nutrition:12,saturation:0.8,bowl:true,ingredients:['minecraft:bowl','agri_evolution:cauliflower','agri_evolution:artichoke','agri_evolution:fennel'],visual:'rustic wooden bowl of golden roasted cauliflower, layered green artichoke hearts and pale fennel wedges'},
 {id:'spicy_peanut_noodles',name:'Spicy Peanut Noodles',category:'meal',nutrition:12,saturation:0.8,bowl:true,ingredients:['minecraft:bowl','minecraft:wheat','agri_evolution:peanut','agri_evolution:chilli','agri_evolution:sesame'],visual:'rustic wooden bowl of golden noodles with tan peanuts, red chilli slices and cream sesame garnish'},
 {id:'berry_oat_crumble',name:'Berry Oat Crumble',category:'dessert',nutrition:9,saturation:0.35,bowl:false,ingredients:['agri_evolution:oats','agri_evolution:gooseberry','agri_evolution:blackcurrant','minecraft:sugar'],visual:'individual golden oat crumble portion with purple blackcurrant filling and green gooseberry, no bowl'}
];
if(process.argv.includes('--spec-only')){fs.writeFileSync('tools/food5-meals.json',JSON.stringify(meals,null,2)+'\n');process.exit(0);}
const edit=(p,f)=>{let s=fs.readFileSync(p,'utf8'),n=f(s);if(n===s)throw Error('No change '+p);fs.writeFileSync(p,n);};
if(fs.readFileSync(j+'content/CookingFoods.java','utf8').includes('green_garden_broth'))throw Error('Already applied');
edit(j+'content/CookingFoods.java',s=>s.replace('\n    );',',\n'+meals.map(m=>'        dish('+JSON.stringify(m.id)+','+m.nutrition+','+m.saturation+'f,'+m.bowl+',new String[]{'+m.ingredients.map(JSON.stringify).join(',')+'})').join(',\n')+'\n    );'));
const all=require('./cooking-spec.json');fs.writeFileSync('tools/cooking-spec.json',JSON.stringify(all.concat(meals),null,2)+'\n');
for(const m of meals){
 const write=(p,v)=>fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');
 write(a+'models/item/'+m.id+'.json',{parent:'minecraft:item/generated',textures:{layer0:'agri_evolution:item/'+m.id}});
 write(res+'data/agri_evolution/recipe/'+m.id+'.json',{type:'minecraft:crafting_shapeless',category:'food',ingredients:m.ingredients.map(item=>({item})),result:{id:'agri_evolution:'+m.id,count:1}});
 write(res+'data/agri_evolution/advancement/recipes/food/'+m.id+'.json',{parent:'minecraft:recipes/root',criteria:{has_ingredient:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:m.ingredients.find(i=>i.startsWith('agri_evolution:'))}]}},has_recipe:{trigger:'minecraft:recipe_unlocked',conditions:{recipe:'agri_evolution:'+m.id}}},requirements:[['has_ingredient','has_recipe']],rewards:{recipes:['agri_evolution:'+m.id]}});
}
edit(a+'lang/en_us.json',s=>{const o=JSON.parse(s);for(const m of meals)o['item.agri_evolution.'+m.id]=m.name;return JSON.stringify(o,null,2)+'\n';});
const pages=[
 ['LEAFY GARDEN','FOOD GARDEN V',['Lettuce + Celery:','Spinach (26%).','Broccoli + Cabbage:','Cauliflower (24%).','Asparagus + Cauliflower:','Artichoke (22%).'],['Onion + Leek:','Spring Onion (26%).','Celery + Parsnip:','Fennel (24%).','Parsley + Fennel:','Dill (26%).']],
 ['GRAINS AND BERRIES','FOOD GARDEN V',['Pepper + Ginger:','Chilli (24%).','Beans + Sweet Potato:','Peanut (24%).','Peanut + Rice:','Sesame (22%).'],['Wild Wheat + Rice:','Oats (26%).','Cranberry + Cucumber:','Gooseberry (22%).','Blackberry + Blueberry:','Blackcurrant (22%).']],
 ['GARDEN KITCHEN V','HARVEST TO TABLE',['Wait for growth stage 7.','Each crop yields edible','produce and genetic seeds.','All fit one farmland cell.','Greenhouses can harvest','and replant all twelve.'],['Try Green Garden Broth,','Roasted Vegetable Bowl,','Spicy Peanut Noodles or','Berry Oat Crumble.','Find illustrated recipes','in this book or Search.']]
];
edit(j+'client/journal/MachineGuidePages.java',s=>s.replace('private static final Page[] PAGES = {','private static final Page[] PAGES = {\n'+pages.map(([t,sub,l,r])=>'        new Page('+JSON.stringify(t)+','+JSON.stringify(sub)+',new String[]{'+l.map(JSON.stringify).join(',')+'},new String[]{'+r.map(JSON.stringify).join(',')+'}),').join('\n')).replace('PLANT_PAGES=java.util.Set.of(','PLANT_PAGES=java.util.Set.of('+pages.map(p=>JSON.stringify(p[0])).join(',')+','));
console.log('Four new dishes and crop-only guide pages added.');
