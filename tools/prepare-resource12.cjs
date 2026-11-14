const fs=require('fs');
const {names,field,lab}=require('./resource12-spec.json');
const sources=['SpeciesBreeder','AdvancedHybridBreeder'].map(f=>fs.readFileSync('src/main/java/com/agrievolution/genetics/'+f+'.java','utf8')).join('\n');
const pairs=[...sources.matchAll(/pair\(first, second, CropSpecies\.(\w+), CropSpecies\.(\w+)\)/g)].map(m=>[m[1],m[2]].sort().join('/'));
for(const [a,b]of [...field,...lab])if(pairs.includes([a,b].sort().join('/')))throw Error('Duplicate parents '+a+' '+b);
let s=fs.readFileSync('tools/add-resource-expansion11.cjs','utf8')
 .replace(/const names=.*?;/,'const names='+JSON.stringify(names)+';')
 .replaceAll('resource-expansion-v11','resource-expansion-v12')
 .replace("includes('WHITE_DYE(')","includes('STICK(')")
 .replaceAll('"experience_bottle_seed", true','"heavy_core_seed", true')
 .replaceAll('AEBlocks.EXPERIENCE_BOTTLE_CROP','AEBlocks.HEAVY_CORE_CROP')
 .replace(/const crosses=.*?;/,'const crosses='+JSON.stringify(field)+';')
 .replace(/const labCrosses=.*?;/,'const labCrosses='+JSON.stringify(lab)+';')
 .replace('["heart_of_the_sea","nether_star","dragon_head","heavy_core"]',JSON.stringify(names.slice(8)))
 .replace("s.replace('mod_version=0.58.0','mod_version=0.59.0')","s.replace('mod_version=0.59.0','mod_version=0.60.0')")
 .replace('additive 0.59.0','additive 0.60.0');
fs.writeFileSync('tools/add-resource-expansion12.cjs',s);
s=fs.readFileSync('tools/finish-resource11.cjs','utf8')
 .replace('./resource11-spec.json','./resource12-spec.json')
 .replace(/const blocks=.*?;/,'const blocks='+JSON.stringify(['OAK_LOG','DRIED_KELP_BLOCK','GRAY_TERRACOTTA','LIGHT_GRAY_TERRACOTTA','CALCITE','MOSS_BLOCK','BEEHIVE','SMOOTH_QUARTZ','BONE_BLOCK','SOUL_SAND','TNT','MOSSY_COBBLESTONE'])+';')
 .replace('const speeds=[2,2,2,2,3,3,3,3,4,5,5,5]','const speeds=[2,2,2,2,3,3,3,3,4,5,4,4]')
 .replace('case WHITE_DYE ->','case STICK ->')
 .replace("'<= EXPERIENCE_BOTTLE.ordinal()','<= HEAVY_CORE.ordinal()'","'<= HEAVY_CORE.ordinal()','<= ZOMBIE_HEAD.ordinal()'")
 .replace("'count()!=133','count()!=145'","'count()!=145','count()!=157'")
 .replace("'CropSpecies.WARPED_FUNGUS.ordinal()!=154','CropSpecies.WARPED_FUNGUS.ordinal()!=154||CropSpecies.EXPERIENCE_BOTTLE.ordinal()!=166'","'CropSpecies.EXPERIENCE_BOTTLE.ordinal()!=166','CropSpecies.EXPERIENCE_BOTTLE.ordinal()!=166||CropSpecies.HEAVY_CORE.ordinal()!=178'")
 .replace("'length != 167','length != 179'","'length != 179','length != 191'")
 .replace("'CropSpecies.EXPERIENCE_BOTTLE','CropSpecies.HEAVY_CORE'","'CropSpecies.HEAVY_CORE','CropSpecies.ZOMBIE_HEAD'")
 .replace("'== 42','== 45'","'== 45','== 48'")
 .replace("'forty-two species','forty-five species'","'forty-five species','forty-eight species'")
 .replace("'==164','==176'","'==176','==188'").replace("'<=164','<=176'","'<=176','<=188'")
 .replace("'all 162 crosses','all 174 crosses'","'all 174 crosses','all 186 crosses'")
 .replace("'==108','==116'","'==116','==124'").replace("'==54','==58'","'==58','==62'")
 .replace("'all 167 breeding','all 179 breeding'","'all 179 breeding','all 191 breeding'")
 .replaceAll('ResourceExpansionEleven','ResourceExpansionTwelve').replaceAll('ResourceExpansionTen','ResourceExpansionEleven')
 .replaceAll('>=CropSpecies.GREEN_DYE.ordinal()','>=CropSpecies.WHITE_DYE.ordinal()')
 .replaceAll('&&species.ordinal()<=CropSpecies.EXPERIENCE_BOTTLE.ordinal()','&&species.ordinal()<=CropSpecies.HEAVY_CORE.ordinal()')
 .replace("'>=CropSpecies.WHITE_DYE.ordinal()','>=CropSpecies.WHITE_DYE.ordinal()'","'>=CropSpecies.WHITE_DYE.ordinal()','>=CropSpecies.STICK.ordinal()'")
 .replace('species==CropSpecies.FIRE_CHARGE||species==CropSpecies.WIND_CHARGE||species==CropSpecies.DRAGON_BREATH||species==CropSpecies.EXPERIENCE_BOTTLE','species==CropSpecies.HEART_OF_THE_SEA||species==CropSpecies.NETHER_STAR||species==CropSpecies.DRAGON_HEAD||species==CropSpecies.HEAVY_CORE')
 .replace("'CropSpecies.GREEN_DYE.ordinal()','CropSpecies.WHITE_DYE.ordinal()'","'CropSpecies.WHITE_DYE.ordinal()','CropSpecies.STICK.ordinal()'");
fs.writeFileSync('tools/finish-resource12.cjs',s);
console.log('All twelve breeding pairs are distinct from existing recipes.');
