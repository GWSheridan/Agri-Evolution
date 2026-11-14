const fs=require('fs'),spec=require('./resource15-spec.json');
let s=fs.readFileSync('tools/add-resource-expansion14.cjs','utf8').replace(/const names=.*?;/,'const names='+JSON.stringify(spec.names)+';').replaceAll('resource-expansion-v14','resource-expansion-v15').replace("includes('GRASS_BLOCK(')","includes('SUGAR_CANE(')").replaceAll('"respawn_anchor_seed", true','"enchanted_golden_apple_seed", true').replaceAll('AEBlocks.RESPAWN_ANCHOR_CROP','AEBlocks.ENCHANTED_GOLDEN_APPLE_CROP').replace(/const crosses=.*?;/,'const crosses='+JSON.stringify(spec.field)+';').replace(/const labCrosses=.*?;/,'const labCrosses='+JSON.stringify(spec.lab)+';').replace('["calibrated_sculk_sensor","end_crystal","totem_of_undying","enchanted_golden_apple"]',JSON.stringify(spec.names.slice(8))).replace("s.replace('mod_version=0.67.1','mod_version=0.68.0')","s.replace('mod_version=0.68.0','mod_version=0.69.0')");fs.writeFileSync('tools/add-resource-expansion15.cjs',s);
s=fs.readFileSync('tools/finish-resource14.cjs','utf8').replace('./resource14-spec.json','./resource15-spec.json').replace("'ordinal() <= RESPAWN_ANCHOR.ordinal());','ordinal() <= ENCHANTED_GOLDEN_APPLE.ordinal());'","'ordinal() <= ENCHANTED_GOLDEN_APPLE.ordinal());','ordinal() <= DISC_FRAGMENT_5.ordinal());'").replace("'count()!=169','count()!=181'","'count()!=181','count()!=183'").replace("'length != 239','length != 251'","'length != 251','length != 253'").replace("'CropSpecies.RESPAWN_ANCHOR','CropSpecies.ENCHANTED_GOLDEN_APPLE'","'CropSpecies.ENCHANTED_GOLDEN_APPLE','CropSpecies.DISC_FRAGMENT_5'").replace("'== 60','== 63'","'== 63','== 64'").replace("'==236','==248'","'==248','==250'").replace("'<=236','<=248'","'<=248','<=250'").replace("'==168','==176'","'==176','==175'").replace("'==66','==70'","'==70','==73'").replace('content/ResourceExpansionThirteenGameTests.java','content/ResourceExpansionFourteenGameTests.java').replace("'ResourceExpansionThirteen','ResourceExpansionFourteen'","'ResourceExpansionFourteen','ResourceExpansionFifteen'").replace("'>=CropSpecies.JUNGLE_LOG.ordinal()&&species.ordinal()<=CropSpecies.RESPAWN_ANCHOR.ordinal()','>=CropSpecies.GRASS_BLOCK.ordinal()&&species.ordinal()<=CropSpecies.ENCHANTED_GOLDEN_APPLE.ordinal()'","'>=CropSpecies.GRASS_BLOCK.ordinal()&&species.ordinal()<=CropSpecies.ENCHANTED_GOLDEN_APPLE.ordinal()','>=CropSpecies.SUGAR_CANE.ordinal()&&species.ordinal()<=CropSpecies.DISC_FRAGMENT_5.ordinal()'").replace('species==CropSpecies.SCULK_SENSOR||species==CropSpecies.SCULK_SHRIEKER||species==CropSpecies.CONDUIT||species==CropSpecies.RESPAWN_ANCHOR','species==CropSpecies.CALIBRATED_SCULK_SENSOR||species==CropSpecies.END_CRYSTAL||species==CropSpecies.TOTEM_OF_UNDYING||species==CropSpecies.ENCHANTED_GOLDEN_APPLE').replace("fs.writeFileSync(t+'content/ResourceExpansionFourteenGameTests.java'","fs.writeFileSync(t+'content/ResourceExpansionFifteenGameTests.java'").replace(".replace('new BlockPos(2+index++,20,2)'",".replace('index==10','index==12').replace('new BlockPos(2+index++,20,2)'");fs.writeFileSync('tools/finish-resource15.cjs',s);
const legacy=JSON.parse(fs.readFileSync('backups/resource15-20261110/species-0680.json'));
fs.writeFileSync('src/main/java/com/agrievolution/genetics/RetiredResourceCrops.java',`package com.agrievolution.genetics;
/** Stable IDs for the 0.69 duplicate-resource cleanup; never reorder the legacy table. */
public final class RetiredResourceCrops {
    private RetiredResourceCrops() {}
    public static final java.util.Map<String,String> ALIASES = java.util.Map.ofEntries(
${Object.entries(spec.removed).map(([a,b])=>'        java.util.Map.entry('+JSON.stringify(a)+','+JSON.stringify(b)+')').join(',\n')});
    private static final String[] LEGACY_068 = {${legacy.map(JSON.stringify).join(',')}};
    public static String canonical(String id) { return ALIASES.getOrDefault(id,id); }
    public static CropSpecies species(String id) {
        String current=canonical(id);
        for(var species:CropSpecies.values())if(species.id().equals(current))return species;
        return null;
    }
    public static int legacyMonitorValue(int value) {
        if(value<=0||value>LEGACY_068.length)return 0;
        var species=species(LEGACY_068[value-1]);
        return species==null?0:species.ordinal()+1;
    }
    public static String monitorId(int value) { return value>0&&value<=CropSpecies.values().length?CropSpecies.values()[value-1].id():""; }
    public static int monitorValue(String id) { var species=species(id);return species==null?0:species.ordinal()+1; }
}
`);
