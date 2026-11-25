package com.agrievolution.client.journal;

import com.agrievolution.genetics.*;
import java.util.*;

/** Derive every illustrated cross from gameplay rules, rather than a second recipe list. */
public final class IllustratedBreedingRecipes {
    public enum Method { CRAFT, FIELD, LAB }
    public record Recipe(CropSpecies first,CropSpecies second,String material,CropSpecies result,int chance,Method method){}
    public static final List<Recipe> ALL=build();
    private static List<Recipe> build(){
        var list=new ArrayList<Recipe>();
        list.add(new Recipe(CropSpecies.WILD,null,"minecraft:dirt",CropSpecies.DIRT,100,Method.CRAFT));
        list.add(new Recipe(CropSpecies.WILD,null,"minecraft:oak_log",CropSpecies.WOOD,100,Method.CRAFT));
        var species=CropSpecies.values();
        for(int i=0;i<species.length;i++)for(int j=i+1;j<species.length;j++){
            var a=species[i];var b=species[j];var field=SpeciesBreeder.mutationRecipe(a,b);var lab=AdvancedHybridBreeder.recipe(a,b);
            if(field!=null)list.add(new Recipe(a,b,null,field.offspring(),field.baseChance(),Method.FIELD));
            if(lab!=null)list.add(new Recipe(a,b,null,lab.offspring(),lab.baseChance(),Method.LAB));
        }
        return List.copyOf(list);
    }
    private IllustratedBreedingRecipes(){}
}
