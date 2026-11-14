const fs=require('fs');
const {names,field,lab}=require('./resource10-spec.json');
const sources=['SpeciesBreeder','AdvancedHybridBreeder'].map(f=>fs.readFileSync('src/main/java/com/agrievolution/genetics/'+f+'.java','utf8')).join('\n');
const pairs=[...sources.matchAll(/pair\(first, second, CropSpecies\.(\w+), CropSpecies\.(\w+)\)/g)].map(m=>[m[1],m[2]].sort().join('/'));
for(const [a,b]of [...field,...lab])if(pairs.includes([a,b].sort().join('/')))throw Error('Duplicate parents '+a+' '+b);
let s=fs.readFileSync('tools/add-resource-expansion9.cjs','utf8')
 .replace(/const names=.*?;/,'const names='+JSON.stringify(names)+';')
 .replaceAll('resource-expansion-v9','resource-expansion-v10')
 .replace("includes('SUGAR(')","includes('GREEN_DYE(')")
 .replaceAll('"pearlescent_froglight_seed", true','"warped_fungus_seed", true')
 .replaceAll('AEBlocks.PEARLESCENT_FROGLIGHT_CROP','AEBlocks.WARPED_FUNGUS_CROP')
 .replace(/const crosses=.*?;/,'const crosses='+JSON.stringify(field)+';')
 .replace(/const labCrosses=.*?;/,'const labCrosses='+JSON.stringify(lab)+';')
 .replace('["wither_rose","chorus_flower","crimson_fungus","warped_fungus"]',JSON.stringify(names.slice(8)))
 .replace("s.replace('mod_version=0.56.0','mod_version=0.57.0')","s.replace('mod_version=0.57.0','mod_version=0.58.0')")
 .replace('additive 0.57.0','additive 0.58.0');
fs.writeFileSync('tools/add-resource-expansion10.cjs',s);
s=fs.readFileSync('tools/finish-resource9.cjs','utf8')
 .replace('./resource9-spec.json','./resource10-spec.json')
 .replace(/const blocks=.*?;/,'const blocks='+JSON.stringify(['GREEN_TERRACOTTA','RED_TERRACOTTA','YELLOW_TERRACOTTA','BROWN_TERRACOTTA','HONEY_BLOCK','MOSS_BLOCK','BLUE_TERRACOTTA','CYAN_TERRACOTTA','MAGMA_BLOCK','TUFF','END_STONE','EMERALD_BLOCK'])+';')
 .replace('const speeds=[2,2,2,2,3,3,3,3,4,4,4,4]','const speeds=[2,2,2,2,3,3,3,3,4,4,5,5]')
 .replace('case SUGAR ->','case GREEN_DYE ->')
 .replace("'<= PEARLESCENT_FROGLIGHT.ordinal()','<= WARPED_FUNGUS.ordinal()'","'<= WARPED_FUNGUS.ordinal()','<= EXPERIENCE_BOTTLE.ordinal()'")
 .replace("'count()!=109','count()!=121'","'count()!=121','count()!=133'")
 .replace("'CropSpecies.OCHRE_FROGLIGHT.ordinal()!=130','CropSpecies.OCHRE_FROGLIGHT.ordinal()!=130||CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()!=142'","'CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()!=142','CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()!=142||CropSpecies.WARPED_FUNGUS.ordinal()!=154'")
 .replace("'length != 143','length != 155'","'length != 155','length != 167'")
 .replace("'CropSpecies.PEARLESCENT_FROGLIGHT','CropSpecies.WARPED_FUNGUS'","'CropSpecies.WARPED_FUNGUS','CropSpecies.EXPERIENCE_BOTTLE'")
 .replace("'== 36','== 39'","'== 39','== 42'")
 .replace("'thirty-six species','thirty-nine species'","'thirty-nine species','forty-two species'")
 .replace("'==140','==152'","'==152','==164'").replace("'<=140','<=152'","'<=152','<=164'")
 .replace("'all 138 crosses','all 150 crosses'","'all 150 crosses','all 162 crosses'")
 .replace("'==92','==100'","'==100','==108'").replace("'==46','==50'","'==50','==54'")
 .replace("'all 143 breeding','all 155 breeding'","'all 155 breeding','all 167 breeding'")
 .replaceAll('ResourceExpansionNine','ResourceExpansionTen').replaceAll('ResourceExpansionEight','ResourceExpansionNine')
 .replaceAll('>=CropSpecies.SPRUCE_LOG.ordinal()','>=CropSpecies.SUGAR.ordinal()')
 .replaceAll('&&species.ordinal()<=CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()','&&species.ordinal()<=CropSpecies.WARPED_FUNGUS.ordinal()')
 .replace("'>=CropSpecies.SUGAR.ordinal()','>=CropSpecies.SUGAR.ordinal()'","'>=CropSpecies.SUGAR.ordinal()','>=CropSpecies.GREEN_DYE.ordinal()'")
 .replace('species==CropSpecies.CRIMSON_STEM||species==CropSpecies.WARPED_STEM||species==CropSpecies.VERDANT_FROGLIGHT||species==CropSpecies.PEARLESCENT_FROGLIGHT','species==CropSpecies.WITHER_ROSE||species==CropSpecies.CHORUS_FLOWER||species==CropSpecies.CRIMSON_FUNGUS||species==CropSpecies.WARPED_FUNGUS')
 .replace("'CropSpecies.SPRUCE_LOG.ordinal()','CropSpecies.SUGAR.ordinal()'","'CropSpecies.SUGAR.ordinal()','CropSpecies.GREEN_DYE.ordinal()'");
fs.writeFileSync('tools/finish-resource10.cjs',s);
console.log('All twelve breeding pairs are distinct from existing recipes.');
