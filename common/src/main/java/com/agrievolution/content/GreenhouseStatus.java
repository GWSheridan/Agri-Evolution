package com.agrievolution.content;

import java.util.List;

/** Read-only interpretations of the already synchronized greenhouse dashboard data. */
public final class GreenhouseStatus {
    public record Indicator(String name,String state,String item,int severity,String help) {}
    public static List<Indicator> alerts(BotanicalGreenhouseMenu menu){
        return indicators(menu).stream().filter(i->i.severity()==1||i.severity()==2)
                .sorted(java.util.Comparator.comparingInt(Indicator::severity).reversed()).toList();
    }
    public static int targetTab(String name){return switch(name){case "Structure"->3;case "Water"->1;case "Storage","Automation"->5;case "Seeds"->6;case "Climate"->4;default->0;};}
    public static List<Indicator> indicators(BotanicalGreenhouseMenu m) {
        boolean climate=m.temperatureC()>=19&&m.temperatureC()<=28&&m.humidityPct()>=45&&m.humidityPct()<=78;
        return List.of(
            new Indicator("Structure",m.formed()?"SEALED":"INCOMPLETE","minecraft:glass",m.formed()?0:2,"Build the sealed 7x7 shell, compatible door and roof. Follow the controller hologram."),
            new Indicator("Water",m.water()==0?"EMPTY":m.lowWater()?"LOW":"READY","minecraft:water_bucket",m.water()==0?2:m.lowWater()?1:0,"Water: "+m.water()+" / "+m.capacity()+" mB. Use water buckets on the controller to refill."),
            new Indicator("Storage",!m.upgraded()?"TIER 2":m.storageFull()?"FULL":"SPACE","minecraft:chest",!m.upgraded()?3:m.storageFull()?2:0,"Harvest storage: "+m.storageUsed()+" / 27 slots used. Remove stored items if harvesting is blocked."),
            new Indicator("Seeds",!m.smartFarmingUnlocked()?"TIER 3":!m.smartPlantEnabled()?"OFF":m.seedCount()==0?"EMPTY":m.monitorEligible()==0?"UNAVAILABLE":"READY","minecraft:wheat_seeds",!m.smartFarmingUnlocked()?3:!m.smartPlantEnabled()?3:m.seedCount()==0?2:m.monitorEligible()==0?1:0,"Check Seed Bank contents, enabled slots, reserves and crop filters. Ready slots: "+m.monitorEligible()+". Disabled: "+m.monitorDisabled()+". Reserved: "+m.monitorReserved()+". Filtered: "+m.monitorFiltered()+"."),
            new Indicator("Climate",!m.active()?"PAUSED":climate?"OPTIMAL":"ADJUST","minecraft:torch",!m.active()?3:climate?0:1,"Target 19-28 C and 45-78% humidity. Use Climate controls and keep heater fuel stocked."),
            new Indicator("Automation",!m.upgraded()?"TIER 2":!m.active()?"PAUSED":m.storageFull()&&m.harvesting()?"BLOCKED":m.harvesting()||m.smartFarmingUnlocked()&&m.smartPlantEnabled()?"ON":"OFF","minecraft:redstone",!m.upgraded()?3:!m.active()?1:m.storageFull()&&m.harvesting()?2:0,"Harvest, Replant and Auto-plant are separate switches. Empty full storage to resume harvesting. Click to open controls; settings are preserved.")
        );
    }
    private GreenhouseStatus() {}
}
