package com.agrievolution.client.journal;

import com.agrievolution.genetics.CropSpecies;

/** Navigation is UI state only: no world or item data is modified. */
public final class JournalNavigation {
    public enum Section {
        OVERVIEW("Overview"), SPECIES("Species"), BREEDING("Breeding"), NOTES("Field Notes"), MACHINES("Instructions"), TIPS("Tips & Tricks");
        private final String title;
        Section(String title) { this.title = title; }
        public String title() { return title; }
    }

    private Section section = Section.OVERVIEW;
    private final boolean building;
    public JournalNavigation(){this(false);}
    public JournalNavigation(boolean building){this.building=building;if(building)section=Section.MACHINES;}
    public boolean building(){return building;}
    private int tipsPage;
    public int tipsPage(){return tipsPage;}
    public void cycleTips(int direction){tipsPage=Math.floorMod(tipsPage+direction,GrandmasTips.PAGES.length);section=Section.TIPS;}
    public Section[] sections(){return building?new Section[]{Section.MACHINES}:Section.values();}
    public String sectionTitle(Section section){return section.title();}
    public MachineGuidePages.Page guidePage(){return MachineGuidePages.forBook(building)[machinePage];}
    private int speciesIndex;
    private int breedingPage;
    public int breedingPage() { return breedingPage; }
    public int breedingPageCount() { return BreedingGuidePages.count(); }
    public void cycleBreedingPage(int direction) {
        int count = breedingPageCount();
        breedingPage = (breedingPage + direction % count + count) % count;
        section = Section.BREEDING;
    }
    private int machinePage;
    public int machinePage() { return machinePage; }
    public int machinePageCount() { return MachineGuidePages.forBook(building).length; }
    public void cycleMachinePage(int direction) {
        int count = machinePageCount();
        machinePage = (machinePage + direction % count + count) % count;
        section = Section.MACHINES;
    }

    public Section section() { return section; }
    public static final int SPECIES_PER_PAGE = 4;
    public int speciesIndex() { return speciesIndex; }
    public int speciesPageStart() { return (speciesIndex / SPECIES_PER_PAGE) * SPECIES_PER_PAGE; }
    public int speciesPageNumber() { return speciesIndex / SPECIES_PER_PAGE + 1; }
    public int speciesPageCount() {
        return (CropSpecies.values().length + SPECIES_PER_PAGE - 1) / SPECIES_PER_PAGE;
    }
    public CropSpecies selectedSpecies() { return CropSpecies.values()[speciesIndex]; }
    public void section(Section section) { if(!building||section==Section.MACHINES)this.section = section; }
    public void selectSpecies(int index) {
        speciesIndex = Math.max(0, Math.min(CropSpecies.values().length - 1, index));
        section = Section.SPECIES;
    }
    public void cycleSpecies(int direction) {
        int count = CropSpecies.values().length;
        speciesIndex = (speciesIndex + direction % count + count) % count;
        section = Section.SPECIES;
    }
    public void cycleSection(int direction) {
        Section[] sections = sections();
        int index=java.util.Arrays.asList(sections).indexOf(section);
        section = sections[(index + direction % sections.length + sections.length) % sections.length];
    }
}
