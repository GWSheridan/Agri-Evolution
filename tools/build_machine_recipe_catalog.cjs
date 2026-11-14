const fs=require('fs');const dir='src/main/resources/data/agri_evolution/recipe/';let recipes=[];
for(const file of fs.readdirSync(dir).sort()){
 const r=JSON.parse(fs.readFileSync(dir+file));if(!['minecraft:crafting_shaped','minecraft:crafting_shapeless'].includes(r.type)||r.result.id.endsWith('_seed'))continue;
 const entry=i=>i.item??'#'+i.tag;
 let width=r.pattern?.[0].length??3,ingredients=r.pattern?r.pattern.join('').split('').map(c=>c===' '?'':entry(r.key[c])):r.ingredients.map(entry);
 recipes.push({id:file.slice(0,-5),width,ingredients,result:r.result.id,count:r.result.count??1});
}
const q=s=>JSON.stringify(s);fs.writeFileSync('src/main/java/com/agrievolution/client/journal/MachineRecipeCatalog.java',`package com.agrievolution.client.journal;
/** Generated from the actual crafting JSON by tools/build_machine_recipe_catalog.cjs. */
public final class MachineRecipeCatalog {
 public record Recipe(String id,int width,String[] ingredients,String result,int count){}
 public static final Recipe[] ALL={
 ${recipes.map(r=>`new Recipe(${q(r.id)},${r.width},new String[]{${r.ingredients.map(q).join(',')}},${q(r.result)},${r.count})`).join(',\n ')}
 };
 private static final java.util.Set<String> FOOD_IDS=java.util.Set.of(${require('./cooking-spec.json').map(m=>q(m.id)).join(',')});
 public static boolean isFood(String id){return FOOD_IDS.contains(id);}
 private MachineRecipeCatalog(){}
}
`);
console.log('Illustrated machine/tool recipes: '+recipes.length);
