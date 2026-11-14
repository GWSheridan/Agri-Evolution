package com.agrievolution.client.journal;

public final class GrandmasTips {
    public record Tip(String title,String subtitle,String[] left,String[] right,int diagram){}
    public static final Tip[] PAGES={
        new Tip("YOUR FIRST CROSS","PICK TWO PARENTS",new String[]{"Grow both crops to maturity.","Sneak-use this journal on A.","Then use it on B nearby."},new String[]{"Keep parents within 8 blocks.","Either parent order works.","Collect the offspring seed.","Replant before crossing again."},0),
        new Tip("KEEP THE GOOD SEEDS","BREED FOR BETTER GENES",new String[]{"Inspect and compare parents.","Growth helps crop speed.","Yield helps harvest quantity."},new String[]{"Save your strongest seeds.","Resilience helps hardiness.","Crossing blends parent genes.","A failed mutation can still", "give useful parent offspring."},1),
        new Tip("ODDS ARE NOT PROMISES","GIVE IT ANOTHER TRY",new String[]{"Recipe pictures show base odds.","20% does not mean every fifth.","A miss gives a parent species."},new String[]{"Support both field parents", "for +5 / +8 / +12 points.","Lab: +8 / +12 / +16 points.","Active greenhouse: lab +5.","Keep spare seeds for retries."},2),
        new Tip("GIVE ROOTS A GOOD HOME","LIGHT, WATER & SOIL",new String[]{"Keep farmland hydrated.","Provide enough light.","Clear weeds with a rake."},new String[]{"Put a matching catalyst", "below the farmland, not beside.","The crop sits two blocks above.","Use each crop's matching block.","See Instructions for catalysts."},3),
        new Tip("FOLLOW THE FAMILY TREE","UNLOCK RARE CROPS",new String[]{"Start with Dirt and Wood seeds.","Save each new species.","It may be your next parent."},new String[]{"Illustrated recipes name the", "required breeding method.","Tier 3 crosses need the lab", "and one Nutrient Compost.","Machine setup: other Journal."},4)
    };
    private GrandmasTips(){}
}
