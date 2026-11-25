package com.agrievolution.client.journal;
import java.util.*;
import com.agrievolution.genetics.CropSpecies;

/** Dependency order from the real recipe catalog, without claiming discovery equals inventory. */
public final class BreedingPlanner {
    public record Step(CropSpecies species,IllustratedBreedingRecipes.Recipe recipe){}
    public static List<Step> plan(CropSpecies target){
        var result=new ArrayList<Step>();visit(target,EnumSet.noneOf(CropSpecies.class),EnumSet.noneOf(CropSpecies.class),result);return List.copyOf(result);
    }
    /** Discovered ancestors already satisfy the journal plan; this is not an inventory check. */
    public static List<Step> remaining(CropSpecies target,java.util.function.Predicate<CropSpecies> discovered){
        var result=new ArrayList<Step>();
        visitRemaining(target,discovered,EnumSet.noneOf(CropSpecies.class),result);
        return List.copyOf(result);
    }
    private static void visitRemaining(CropSpecies species,java.util.function.Predicate<CropSpecies> discovered,Set<CropSpecies> seen,List<Step> result){
        if(discovered.test(species)||!seen.add(species))return;
        var recipe=recipeFor(species);
        if(recipe!=null){visitRemaining(recipe.first(),discovered,seen,result);if(recipe.second()!=null)visitRemaining(recipe.second(),discovered,seen,result);}
        result.add(new Step(species,recipe));
    }
    public static List<CropSpecies> missingParents(Step step,java.util.function.Predicate<CropSpecies> discovered){
        if(step.recipe()==null)return List.of();
        var result=new ArrayList<CropSpecies>();
        if(!discovered.test(step.recipe().first()))result.add(step.recipe().first());
        if(step.recipe().second()!=null&&!discovered.test(step.recipe().second()))result.add(step.recipe().second());
        return List.copyOf(result);
    }
    public static String method(Step step){
        if(step.recipe()==null)return "Starter seed";
        return switch(step.recipe().method()){
            case CRAFT -> "Crafting grid";
            case FIELD -> "Field crossing / Breeding Station";
            case LAB -> "Tier 3 Genetics Laboratory";
        };
    }
    private static void visit(CropSpecies species,Set<CropSpecies> done,Set<CropSpecies> active,List<Step> steps){
        if(done.contains(species))return;
        if(!active.add(species))throw new IllegalStateException("Cyclic breeding recipe: "+species);
        var recipe=recipeFor(species);
        if(recipe!=null){visit(recipe.first(),done,active,steps);if(recipe.second()!=null)visit(recipe.second(),done,active,steps);}
        active.remove(species);done.add(species);steps.add(new Step(species,recipe));
    }
    public static String requirements(Step step){
        var r=step.recipe();if(r==null)return "Break short or tall grass in the Overworld for Wild Seeds, then record the planted crop.";
        return r.first().displayName()+" + "+(r.second()==null?r.material().replace("minecraft:","").replace('_',' '):r.second().displayName())+" | "+method(step)+" | Base chance: "+r.chance()+"%";
    }
    private static IllustratedBreedingRecipes.Recipe recipeFor(CropSpecies species){
        // Existing shapeless starter recipes, also checked against the packaged recipe JSON.
        if(species==CropSpecies.CRIMSON_CLOVER||species==CropSpecies.AZURE_FLAX)
            return new IllustratedBreedingRecipes.Recipe(CropSpecies.WILD,null,
                species==CropSpecies.CRIMSON_CLOVER?"minecraft:poppy":"minecraft:cornflower",
                species,100,IllustratedBreedingRecipes.Method.CRAFT);
        return IllustratedBreedingRecipes.ALL.stream().filter(r->r.result()==species).findFirst().orElse(null);
    }
    private BreedingPlanner(){}
}
