package com.agrievolution.client.journal;

import com.agrievolution.genetics.CropSpecies;

/** Plain Java smoke test for tab navigation and discovery-index boundaries. */
public final class JournalNavigationSelfTest {
    public static void main(String[] args) {
        JournalNavigation nav = new JournalNavigation();
        assertState(nav.section() == JournalNavigation.Section.OVERVIEW, "initial tab");
        nav.cycleSection(-1);
        assertState(nav.section() == JournalNavigation.Section.TIPS, "tab wrap backwards includes tips");
        nav.cycleSection(1);
        assertState(nav.section() == JournalNavigation.Section.OVERVIEW, "tab wrap forwards");
        nav.selectSpecies(Integer.MAX_VALUE);
        assertState(nav.selectedSpecies() == CropSpecies.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, "upper bound clamp");
        nav.cycleSpecies(1);
        assertState(nav.selectedSpecies() == CropSpecies.WILD, "species wrap");
        nav.selectSpecies(-5);
        assertState(nav.selectedSpecies() == CropSpecies.WILD, "lower bound clamp");
        nav.cycleSpecies(-1);
        assertState(nav.selectedSpecies() == CropSpecies.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, "species reverse wrap");
        nav.selectSpecies(4);
        assertState(nav.speciesPageNumber() == 2 && nav.speciesPageStart() == 4, "second journal page");
        nav.selectSpecies(3);
        assertState(nav.speciesPageNumber() == 1 && nav.speciesPageStart() == 0, "first journal page");
        nav.cycleSpecies(1);
        assertState(nav.selectedSpecies() == CropSpecies.AMBER_MARIGOLD, "page turn on next specimen");
        assertState(nav.speciesPageCount() == 67, "sixty-seven species pages");
        nav.section(JournalNavigation.Section.MACHINES);
        assertState(nav.machinePage() == 0, "machine guide initial page");
        nav.cycleMachinePage(-1);
        assertState(nav.machinePage() == nav.machinePageCount() - 1, "guide reverse wrap");
        nav.cycleMachinePage(1);
        assertState(nav.machinePage() == 0, "guide forward wrap");
        var building=new JournalNavigation(true);
        assertState(building.section()==JournalNavigation.Section.MACHINES,"building book opens instructions");
        building.cycleSection(1);building.section(JournalNavigation.Section.SPECIES);
        assertState(building.section()==JournalNavigation.Section.MACHINES&&building.sections().length==1,"building book cannot enter crop sections");
        var plantTitles=new java.util.HashSet<String>();
        assertState(nav.sectionTitle(JournalNavigation.Section.MACHINES).equals("Instructions"),"retain original Instructions tab label");
        for(var page:MachineGuidePages.forBook(false))plantTitles.add(page.title());
        assertState(plantTitles.contains("READ THIS FIRST")&&plantTitles.contains("IF IT DOES NOT WORK"),"keep plant starter and troubleshooting instructions in breeding journal");
        assertState(plantTitles.contains("FIRST WILD SEEDS")&&!plantTitles.contains("GREENHOUSE BUILD")&&!plantTitles.contains("COPPER RESERVOIR"),"plant guide excludes machine builds");
        for(var page:MachineGuidePages.forBook(true))assertState(!plantTitles.contains(page.title()),"instruction pages must belong to only one book");
        var machineTitles=new java.util.HashSet<String>();
        for(var page:MachineGuidePages.forBook(true))machineTitles.add(page.title());
        for(var title:java.util.List.of("SEED BAGS","WOODEN & IRON RAKES","GARDENING TROWEL","BREEDING STATION","IRRIGATION PIPES","GARDENING SPANNER"))
            assertState(machineTitles.contains(title)&&!plantTitles.contains(title),"tool and machine instructions belong only in Journal: "+title);
        assertState(plantTitles.contains("ADVANCED SOIL TIPS")&&!machineTitles.contains("RESOURCE CATALYSTS"),"growing advice belongs in Grandmas Journal");
        assertState(building.machinePageCount()+nav.machinePageCount()==MachineGuidePages.count(),"all existing instruction pages remain accessible");
        assertState(IllustratedBreedingRecipes.ALL.size()==262,"all 260 crosses and two starter recipes illustrated");
        assertState(IllustratedBreedingRecipes.ALL.stream().filter(r->r.method()==IllustratedBreedingRecipes.Method.FIELD).count()==183,"all field/station crosses pictured");
        assertState(IllustratedBreedingRecipes.ALL.stream().filter(r->r.method()==IllustratedBreedingRecipes.Method.LAB).count()==77,"all Tier 3 crosses pictured");
        for(int i=1;i<=262;i++)assertState(BreedingGuidePages.recipe(i)!=null,"every recipe page has visual ingredients and result");
        nav.cycleTips(-1);assertState(nav.tipsPage()==GrandmasTips.PAGES.length-1,"tips backwards wrap");
        nav.cycleTips(1);assertState(nav.tipsPage()==0,"tips forward wrap");
        for (int i = 0; i < MachineGuidePages.count(); i++) {
            MachineGuidePages.Page page = MachineGuidePages.get(i);
            assertState(page.left().length <= 6 && page.right().length <= 6, "guide fits vertically");
            // Horizontal fit uses the loaded client font in JournalLayoutChecks.
            // Character counts do not reflect the uniform font's glyph widths.
        }
        var machineIndex=JournalIndex.entries(true);var cropIndex=JournalIndex.entries(false);
        for(var recipe:MachineRecipeCatalog.ALL)if(MachineRecipeCatalog.isFood(recipe.id())){
            String title="Craft: "+recipe.id().replace('_',' ');
            assertState(cropIndex.stream().anyMatch(e->e.title().equals(title)),"Food recipe belongs in Grandmas Journal");
            assertState(machineIndex.stream().noneMatch(e->e.title().equals(title)),"Food recipe excluded from machine Journal");
        }
        assertState(machineIndex.stream().allMatch(e->e.section()==JournalNavigation.Section.MACHINES),"machine search cannot expose crop sections");
        assertState(machineIndex.stream().anyMatch(e->e.matches("  IRON_CROP_SUPPORT  ")),"search includes crafting ingredient IDs and ignores case/outer spaces");
        assertState(cropIndex.stream().noneMatch(e->e.title().equals("COPPER RESERVOIR")),"crop search excludes machine instructions");
        for(var entry:machineIndex){entry.open(building);assertState(building.machinePage()==entry.page(),"index opens exact machine page");}
        for(var entry:cropIndex){entry.open(nav);assertState(nav.section()==entry.section(),"index opens correct crop section");}
        assertState(cropIndex.stream().filter(e->e.section()==JournalNavigation.Section.BREEDING&&BreedingGuidePages.recipe(e.page())!=null).count()==262,"tracker covers every recipe");
        assertState(machineIndex.stream().map(JournalIndex.Entry::key).distinct().count()==machineIndex.size(),"stable machine bookmark keys unique");
        assertState(cropIndex.stream().map(JournalIndex.Entry::key).distinct().count()==cropIndex.size(),"stable crop bookmark keys unique");
        for(var target:com.agrievolution.genetics.CropSpecies.values()){
            var plan=BreedingPlanner.plan(target);var seen=new java.util.HashSet<com.agrievolution.genetics.CropSpecies>();
            for(var step:plan){if(step.recipe()!=null){assertState(seen.contains(step.recipe().first()),"first prerequisite precedes result");if(step.recipe().second()!=null)assertState(seen.contains(step.recipe().second()),"second prerequisite precedes result");}assertState(seen.add(step.species()),"shared ancestors appear once");}
            assertState(plan.getLast().species()==target,"planner ends at selected target");
            assertState(BreedingPlanner.remaining(target,s->false).equals(plan),"empty discoveries show complete chain");
            assertState(BreedingPlanner.remaining(target,s->true).isEmpty(),"discovered target needs no steps");
            var parentsKnown=BreedingPlanner.remaining(target,s->s!=target);
            assertState(parentsKnown.size()==1&&parentsKnown.getFirst().species()==target,"known ancestors prune their own dependencies");
            for(var step:plan){
                assertState(BreedingPlanner.missingParents(step,s->true).isEmpty(),"known parents are not reported missing");
                if(step.recipe()!=null)assertState(BreedingPlanner.missingParents(step,s->false).size()==(step.recipe().second()==null?1:2),"all missing direct parents listed");
            }
        }
        for(var starter:new CropSpecies[]{CropSpecies.CRIMSON_CLOVER,CropSpecies.AZURE_FLAX}){
            var recipe=BreedingPlanner.plan(starter).getLast().recipe();
            try(var stream=JournalNavigationSelfTest.class.getResourceAsStream("/data/agri_evolution/recipe/"+starter.seedId()+".json")){
                if(stream==null)throw new AssertionError("Missing packaged starter recipe");
                var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                var ingredients=json.getAsJsonArray("ingredients");
                assertState(json.get("type").getAsString().equals("minecraft:crafting_shapeless")&&ingredients.size()==2,"starter recipe structure");
                assertState(ingredients.get(0).getAsJsonObject().get("item").getAsString().equals("agri_evolution:"+recipe.first().seedId())&&ingredients.get(1).getAsJsonObject().get("item").getAsString().equals(recipe.material()),"planner matches actual crafting ingredients");
                assertState(json.getAsJsonObject("result").get("id").getAsString().equals("agri_evolution:"+starter.seedId()),"planner matches actual crafting output");
            }catch(java.io.IOException error){throw new AssertionError(error);}
        }
        System.out.println("Agri Evolution: all 265 breeding plans and journal navigation tests passed.");
    }

    private static void assertState(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}



