const fs = require('fs');
const directory = 'src/main/resources/data/agri_evolution/recipe/';
const destination = 'src/main/java/com/agrievolution/client/journal/MachineRecipeCatalog.java';

function alternatives(ingredient) {
    if (Array.isArray(ingredient)) return ingredient.flatMap(alternatives);
    if (ingredient.item) return [ingredient.item];
    if (ingredient.tag) return ['#' + ingredient.tag];
    throw new Error('Unsupported journal ingredient: ' + JSON.stringify(ingredient));
}

const recipes = fs.readdirSync(directory).sort().flatMap(file => {
    const recipe = JSON.parse(fs.readFileSync(directory + file, 'utf8'));
    if (!['minecraft:crafting_shaped', 'minecraft:crafting_shapeless'].includes(recipe.type)
            || recipe.result.id.endsWith('_seed')) return [];
    const shaped = Array.isArray(recipe.pattern);
    const ingredients = shaped
        ? recipe.pattern.join('').split('').map(symbol => symbol === ' ' ? [] : alternatives(recipe.key[symbol]))
        : recipe.ingredients.map(alternatives);
    return [{id: file.slice(0, -5), width: recipe.pattern?.[0].length ?? 3,
        ingredients, shaped, result: recipe.result.id, count: recipe.result.count ?? 1}];
});
const q = JSON.stringify;
const source = `package com.agrievolution.client.journal;
/** Generated from crafting JSON by tools/build_machine_recipe_catalog.cjs. */
public final class MachineRecipeCatalog {
 public record Recipe(String id,int width,String[][] ingredients,String result,int count,boolean shaped){}
 public static final Recipe[] ALL={
 ${recipes.map(r => `new Recipe(${q(r.id)},${r.width},new String[][]{${r.ingredients.map(a => `new String[]{${a.map(q).join(',')}}`).join(',')}},${q(r.result)},${r.count},${r.shaped})`).join(',\n ')}
 };
 private static final java.util.Set<String> FOOD_IDS=java.util.Set.of(${require('./cooking-spec.json').map(m => q(m.id)).join(',')});
 public static boolean isFood(String id){return FOOD_IDS.contains(id);}
 private MachineRecipeCatalog(){}
}
`;
if (process.argv.includes('--check')) {
    const actual = fs.readFileSync(destination, 'utf8').replace(/\r\n/g, '\n');
    if (actual !== source) throw new Error('Journal recipe catalogue is stale. Run node tools/build_machine_recipe_catalog.cjs');
    console.log(`Verified ${recipes.length} illustrated recipes, including ingredient alternatives, shapes and result counts.`);
} else {
    fs.writeFileSync(destination, source);
    console.log(`Updated ${recipes.length} illustrated recipes.`);
}
