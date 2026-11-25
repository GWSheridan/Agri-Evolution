package com.agrievolution.client.journal;

import java.util.*;
import com.agrievolution.genetics.CropSpecies;

/** Search indexes only the current book; recipe progress means result discovery, not a successful-cross log. */
public final class JournalIndex {
    public record Entry(JournalNavigation.Section section,int page,String title,String text){
        public String key(){return section.name()+":"+title;}
        public boolean matches(String query){return (title+" "+text).toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT));}
        public void open(JournalNavigation nav){
            switch(section){
                case SPECIES->nav.selectSpecies(page);
                case BREEDING->nav.cycleBreedingPage(page-nav.breedingPage());
                case MACHINES->nav.cycleMachinePage(page-nav.machinePage());
                case TIPS->nav.cycleTips(page-nav.tipsPage());
                default->nav.section(section);
            }
        }
    }
    public static List<Entry> entries(boolean building){
        var result=new ArrayList<Entry>();var guides=MachineGuidePages.forBook(building);
        for(int i=0;i<guides.length;i++){
            var p=guides[i];String title=p.title(),body=p.subtitle()+" "+String.join(" ",p.left())+" "+String.join(" ",p.right());
            if(title.startsWith("CRAFT ")){var recipe=MachineRecipeCatalog.ALL[Integer.parseInt(title.substring(6))];title="Craft: "+recipe.id().replace('_',' ');body+=" "+java.util.Arrays.stream(recipe.ingredients()).flatMap(java.util.Arrays::stream).collect(java.util.stream.Collectors.joining(" "));}
            result.add(new Entry(JournalNavigation.Section.MACHINES,i,title,body));
        }
        if(!building){
            result.add(new Entry(JournalNavigation.Section.OVERVIEW,0,"Overview","Discovery collection progress"));
            result.add(new Entry(JournalNavigation.Section.NOTES,0,"Field Notes","Genes growth yield resilience rare traits"));
            for(var species:CropSpecies.values())result.add(new Entry(JournalNavigation.Section.SPECIES,species.ordinal(),species.displayName(),"Crop specimen genes discovery"));
            for(int i=0;i<BreedingGuidePages.count();i++){
                var p=BreedingGuidePages.get(i);var recipe=BreedingGuidePages.recipe(i);
                String text=recipe==null?p.subtitle()+" "+String.join(" ",p.left())+" "+String.join(" ",p.right()):recipe.first().displayName()+" + "+(recipe.second()==null?recipe.material():recipe.second().displayName())+" "+recipe.method();
                result.add(new Entry(JournalNavigation.Section.BREEDING,i,p.title(),text));
            }
            for(int i=0;i<GrandmasTips.PAGES.length;i++){var p=GrandmasTips.PAGES[i];result.add(new Entry(JournalNavigation.Section.TIPS,i,p.title(),p.subtitle()+" "+String.join(" ",p.left())+" "+String.join(" ",p.right())));}
        }
        return List.copyOf(result);
    }
    public static String progress(JournalSnapshot snapshot,IllustratedBreedingRecipes.Recipe recipe){
        if(snapshot.entry(recipe.result()).discovered())return "Result discovered";
        var missing=new ArrayList<String>();
        if(!snapshot.entry(recipe.first()).discovered())missing.add(recipe.first().displayName());
        if(recipe.second()!=null&&!snapshot.entry(recipe.second()).discovered())missing.add(recipe.second().displayName());
        return missing.isEmpty()?"Parents discovered - try this recipe":"Undiscovered parent: "+String.join(", ",missing);
    }
    private JournalIndex(){}
}
