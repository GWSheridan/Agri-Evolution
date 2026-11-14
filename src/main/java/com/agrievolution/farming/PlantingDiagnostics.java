package com.agrievolution.farming;

/** Read-only bit flags: a slot can have several simultaneous exclusions. */
public final class PlantingDiagnostics {
    public static final int EMPTY=1, DISABLED=2, CATEGORY=4, SPECIES=8,
            GROWTH=16, YIELD=32, RESILIENCE=64, RESERVED=128;
    private PlantingDiagnostics() { }
    public static java.util.List<String> reasons(int flags) {
        var result=new java.util.ArrayList<String>();
        if((flags&EMPTY)!=0)result.add("No seeds in this slot");
        if((flags&DISABLED)!=0)result.add("Slot disabled: turn it ON");
        if((flags&CATEGORY)!=0)result.add("Category disabled in Planting tab");
        if((flags&SPECIES)!=0)result.add("Wrong species for this slot's lock");
        if((flags&GROWTH)!=0)result.add("Growth below planting minimum");
        if((flags&YIELD)!=0)result.add("Yield below planting minimum");
        if((flags&RESILIENCE)!=0)result.add("Resilience below planting minimum");
        if((flags&RESERVED)!=0)result.add("All seeds in this slot are reserved");
        return result;
    }
}
