const fs=require('fs'),path=require('path'),meals=require('./cooking-spec.json');
const j='src/main/java/com/agrievolution/',res='src/main/resources/',a=res+'assets/agri_evolution/';
const edit=(p,f)=>{const old=fs.readFileSync(p,'utf8'),s=f(old);if(s===old)throw Error('No change '+p);fs.writeFileSync(p,s);};
const write=(p,v)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');};
const reg=fs.readFileSync(j+'genetics/CropSpecies.java','utf8');
const all=[...reg.matchAll(/^    (\w+)\("([^"]+)"/gm)].map(x=>x[2]);
const food=all.slice(39,63).concat(all.slice(191));
const used=new Set(meals.flatMap(x=>x.ingredients));
const missing=food.filter(n=>!used.has('agri_evolution:'+n));
if(missing.length)throw Error('Unused food crops: '+missing);
if(fs.existsSync(j+'content/CookingFoods.java'))throw Error('Already applied');
fs.writeFileSync(j+'content/CookingFoods.java','package com.agrievolution.content;\n\nimport java.util.*;\nimport net.minecraft.world.item.*;\nimport net.minecraft.world.food.FoodProperties;\nimport net.neoforged.neoforge.registries.DeferredItem;\n\n/** Prepared dishes use normal food components and vanilla consumption/remainder handling. */\npublic final class CookingFoods {\n    public record Dish(String id,int nutrition,float saturation,boolean bowl,String[] ingredients,DeferredItem<Item> item) {}\n    public static final List<Dish> ALL=List.of(\n'+meals.map(m=>'        dish("'+m.id+'",'+m.nutrition+','+m.saturation+'f,'+m.bowl+',new String[]{'+m.ingredients.map(x=>JSON.stringify(x)).join(',')+'})').join(',\n')+'\n    );\n    private static Dish dish(String id,int nutrition,float saturation,boolean bowl,String[] ingredients) {\n        var food=new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);\n        if(bowl)food.usingConvertsTo(Items.BOWL);\n        var item=AEItems.ITEMS.registerItem(id,Item::new,new Item.Properties().stacksTo(16).food(food.build()));\n        return new Dish(id,nutrition,saturation,bowl,ingredients,item);\n    }\n    public static void bootstrap() {}\n    public static boolean contains(String id){return ALL.stream().anyMatch(d->d.id().equals(id));}\n    private CookingFoods(){}\n}\n');
edit(j+'content/AEItems.java',s=>s.replace('    public static final DeferredItem<Item> BREEDING_JOURNAL','    static { CookingFoods.bootstrap(); }\n\n    public static final DeferredItem<Item> BREEDING_JOURNAL'));
edit(j+'content/AECreativeTabs.java',s=>s.replace('output.accept(AEItems.BREEDING_JOURNAL.get());','CookingFoods.ALL.forEach(dish -> output.accept(dish.item().get()));\n                        output.accept(AEItems.BREEDING_JOURNAL.get());'));
for(const m of meals){
 write(a+'models/item/'+m.id+'.json',{parent:'minecraft:item/generated',textures:{layer0:'agri_evolution:item/'+m.id}});
 write(res+'data/agri_evolution/recipe/'+m.id+'.json',{type:'minecraft:crafting_shapeless',category:'food',ingredients:m.ingredients.map(item=>({item})),result:{id:'agri_evolution:'+m.id,count:1}});
 write(res+'data/agri_evolution/advancement/recipes/food/'+m.id+'.json',{parent:'minecraft:recipes/root',criteria:{has_ingredient:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:m.ingredients.find(i=>i.startsWith('agri_evolution:'))}]}},has_recipe:{trigger:'minecraft:recipe_unlocked',conditions:{recipe:'agri_evolution:'+m.id}}},requirements:[['has_ingredient','has_recipe']],rewards:{recipes:['agri_evolution:'+m.id]}});
}
edit(a+'lang/en_us.json',s=>{const v=JSON.parse(s);for(const m of meals)v['item.agri_evolution.'+m.id]=m.name;return JSON.stringify(v,null,2)+'\n';});
edit(j+'client/journal/MachineRecipeCatalog.java',s=>s.replace(' };\n private MachineRecipeCatalog()',',\n'+meals.map(m=>' new Recipe("'+m.id+'",3,new String[]{'+m.ingredients.map(x=>JSON.stringify(x)).join(',')+'},"agri_evolution:'+m.id+'",1)').join(',\n')+'\n };\n private MachineRecipeCatalog()'));
edit(j+'client/journal/MachineGuidePages.java',s=>{
 s=s.replace('java.util.stream.IntStream.range(0,MachineRecipeCatalog.ALL.length).mapToObj','java.util.stream.IntStream.range(0,MachineRecipeCatalog.ALL.length).filter(i->!com.agrievolution.content.CookingFoods.contains(MachineRecipeCatalog.ALL[i].id())).mapToObj');
 s=s.replace('private static final Page[] PLANTS=java.util.Arrays.stream(PAGES).filter(p->PLANT_PAGES.contains(p.title())).toArray(Page[]::new);','private static final Page[] PLANTS=java.util.stream.Stream.concat(java.util.Arrays.stream(PAGES).filter(p->PLANT_PAGES.contains(p.title())),java.util.stream.IntStream.range(0,MachineRecipeCatalog.ALL.length).filter(i->com.agrievolution.content.CookingFoods.contains(MachineRecipeCatalog.ALL[i].id())).mapToObj(i->new Page("CRAFT "+i,"GARDEN KITCHEN",new String[0],new String[0]))).toArray(Page[]::new);');
 return s;
});
edit(j+'client/journal/MachineIllustrations.java',s=>s.replace('text(g,f,"Use a crafting table.",right,lineY+29);','text(g,f,com.agrievolution.content.CookingFoods.contains(spec.id())?"Any order in crafting table.":"Use a crafting table.",right,lineY+29);'));
edit('gradle.properties',s=>s.replace('mod_version=0.61.0','mod_version=0.62.0'));
console.log('24 dishes, recipes, unlocks, journal illustrations; all 36 food crops covered.');

