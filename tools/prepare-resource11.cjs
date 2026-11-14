const fs=require('fs');
const {names,field,lab}=require('./resource11-spec.json');
const sources=['SpeciesBreeder','AdvancedHybridBreeder'].map(f=>fs.readFileSync('src/main/java/com/agrievolution/genetics/'+f+'.java','utf8')).join('\n');
const pairs=[...sources.matchAll(/pair\(first, second, CropSpecies\.(\w+), CropSpecies\.(\w+)\)/g)].map(m=>[m[1],m[2]].sort().join('/'));
for(const [a,b]of [...field,...lab])if(pairs.includes([a,b].sort().join('/')))throw Error('Duplicate parents '+a+' '+b);
let s=fs.readFileSync('tools/add-resource-expansion10.cjs','utf8')
 .replace(/const names=.*?;/,'const names='+JSON.stringify(names)+';')
 .replaceAll('resource-expansion-v10','resource-expansion-v11')
 .replace("includes('GREEN_DYE(')","includes('WHITE_DYE(')")
 .replaceAll('"warped_fungus_seed", true','"experience_bottle_seed", true')
 .replaceAll('AEBlocks.WARPED_FUNGUS_CROP','AEBlocks.EXPERIENCE_BOTTLE_CROP')
 .replace(/const crosses=.*?;/,'const crosses='+JSON.stringify(field)+';')
 .replace(/const labCrosses=.*?;/,'const labCrosses='+JSON.stringify(lab)+';')
 .replace('["fire_charge","wind_charge","dragon_breath","experience_bottle"]',JSON.stringify(names.slice(8)))
 .replace("s.replace('mod_version=0.57.0','mod_version=0.58.0')","s.replace('mod_version=0.58.0','mod_version=0.59.0')")
 .replace('additive 0.58.0','additive 0.59.0');
fs.writeFileSync('tools/add-resource-expansion11.cjs',s);
s=fs.readFileSync('tools/finish-resource10.cjs','utf8')
 .replace('./resource10-spec.json','./resource11-spec.json')
 .replace(/const blocks=.*?;/,'const blocks='+JSON.stringify(['WHITE_TERRACOTTA','BLACK_TERRACOTTA','ORANGE_TERRACOTTA','PINK_TERRACOTTA','PURPLE_TERRACOTTA','MAGENTA_TERRACOTTA','LIGHT_BLUE_TERRACOTTA','LIME_TERRACOTTA','DARK_PRISMARINE','SOUL_SAND','END_STONE','CHISELED_TUFF'])+';')
 .replace('const speeds=[2,2,2,2,3,3,3,3,4,4,5,5]','const speeds=[2,2,2,2,3,3,3,3,4,5,5,5]')
 .replace('case GREEN_DYE ->','case WHITE_DYE ->')
 .replace("'<= WARPED_FUNGUS.ordinal()','<= EXPERIENCE_BOTTLE.ordinal()'","'<= EXPERIENCE_BOTTLE.ordinal()','<= HEAVY_CORE.ordinal()'")
 .replace("'count()!=121','count()!=133'","'count()!=133','count()!=145'")
 .replace("'CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()!=142','CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()!=142||CropSpecies.WARPED_FUNGUS.ordinal()!=154'","'CropSpecies.WARPED_FUNGUS.ordinal()!=154','CropSpecies.WARPED_FUNGUS.ordinal()!=154||CropSpecies.EXPERIENCE_BOTTLE.ordinal()!=166'")
 .replace("'length != 155','length != 167'","'length != 167','length != 179'")
 .replace("'CropSpecies.WARPED_FUNGUS','CropSpecies.EXPERIENCE_BOTTLE'","'CropSpecies.EXPERIENCE_BOTTLE','CropSpecies.HEAVY_CORE'")
 .replace("'== 39','== 42'","'== 42','== 45'")
 .replace("'thirty-nine species','forty-two species'","'forty-two species','forty-five species'")
 .replace("'==152','==164'","'==164','==176'").replace("'<=152','<=164'","'<=164','<=176'")
 .replace("'all 150 crosses','all 162 crosses'","'all 162 crosses','all 174 crosses'")
 .replace("'==100','==108'","'==108','==116'").replace("'==50','==54'","'==54','==58'")
 .replace("'all 155 breeding','all 167 breeding'","'all 167 breeding','all 179 breeding'")
 .replaceAll('ResourceExpansionTen','ResourceExpansionEleven').replaceAll('ResourceExpansionNine','ResourceExpansionTen')
 .replaceAll('>=CropSpecies.SUGAR.ordinal()','>=CropSpecies.GREEN_DYE.ordinal()')
 .replaceAll('&&species.ordinal()<=CropSpecies.WARPED_FUNGUS.ordinal()','&&species.ordinal()<=CropSpecies.EXPERIENCE_BOTTLE.ordinal()')
 .replace("'>=CropSpecies.GREEN_DYE.ordinal()','>=CropSpecies.GREEN_DYE.ordinal()'","'>=CropSpecies.GREEN_DYE.ordinal()','>=CropSpecies.WHITE_DYE.ordinal()'")
 .replace('species==CropSpecies.WITHER_ROSE||species==CropSpecies.CHORUS_FLOWER||species==CropSpecies.CRIMSON_FUNGUS||species==CropSpecies.WARPED_FUNGUS','species==CropSpecies.FIRE_CHARGE||species==CropSpecies.WIND_CHARGE||species==CropSpecies.DRAGON_BREATH||species==CropSpecies.EXPERIENCE_BOTTLE')
 .replace("'CropSpecies.SUGAR.ordinal()','CropSpecies.GREEN_DYE.ordinal()'","'CropSpecies.GREEN_DYE.ordinal()','CropSpecies.WHITE_DYE.ordinal()'");
fs.writeFileSync('tools/finish-resource11.cjs',s);
console.log('All twelve breeding pairs are distinct from existing recipes.');
