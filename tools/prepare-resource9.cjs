const fs=require('fs');
const {names,field,lab}=require('./resource9-spec.json');
const sources=['SpeciesBreeder','AdvancedHybridBreeder'].map(f=>fs.readFileSync('src/main/java/com/agrievolution/genetics/'+f+'.java','utf8')).join('\n');
const pairs=[...sources.matchAll(/pair\(first, second, CropSpecies\.(\w+), CropSpecies\.(\w+)\)/g)].map(m=>[m[1],m[2]].sort().join('/'));
for(const [a,b]of [...field,...lab])if(pairs.includes([a,b].sort().join('/')))throw Error('Duplicate parents '+a+' '+b);
let s=fs.readFileSync('tools/add-resource-expansion8.cjs','utf8')
 .replace(/const names=.*?;/,'const names='+JSON.stringify(names)+';')
 .replaceAll('resource-expansion-v8','resource-expansion-v9')
 .replace("includes('SPRUCE_LOG(')","includes('SUGAR(')")
 .replaceAll('"ochre_froglight_seed", true','"pearlescent_froglight_seed", true')
 .replaceAll('AEBlocks.OCHRE_FROGLIGHT_CROP','AEBlocks.PEARLESCENT_FROGLIGHT_CROP')
 .replace(/const crosses=.*?;/,'const crosses='+JSON.stringify(field)+';')
 .replace(/const labCrosses=.*?;/,'const labCrosses='+JSON.stringify(lab)+';')
 .replace('["crimson_stem","warped_stem","verdant_froglight","pearlescent_froglight"]',JSON.stringify(names.slice(8)))
 .replace("s.replace('mod_version=0.55.0','mod_version=0.56.0')","s.replace('mod_version=0.56.0','mod_version=0.57.0')")
 .replace('additive 0.56.0','additive 0.57.0');
fs.writeFileSync('tools/add-resource-expansion9.cjs',s);
s=fs.readFileSync('tools/finish-resource8.cjs','utf8')
 .replace('./resource8-spec.json','./resource9-spec.json')
 .replace(/const blocks=.*?;/,'const blocks='+JSON.stringify(['HAY_BLOCK','JUNGLE_LOG','HAY_BLOCK','MOSS_BLOCK','HAY_BLOCK','MOSS_BLOCK','TERRACOTTA','BROWN_MUSHROOM_BLOCK','SOUL_SAND','END_STONE','CRIMSON_NYLIUM','WARPED_NYLIUM'])+';')
 .replace('const speeds=[2,2,2,2,3,3,3,3,4,4,5,5]','const speeds=[2,2,2,2,3,3,3,3,4,4,4,4]')
 .replace('case SPRUCE_LOG ->','case SUGAR ->')
 .replace("'<= OCHRE_FROGLIGHT.ordinal()','<= PEARLESCENT_FROGLIGHT.ordinal()'","'<= PEARLESCENT_FROGLIGHT.ordinal()','<= WARPED_FUNGUS.ordinal()'")
 .replace("'count()!=97','count()!=109'","'count()!=109','count()!=121'")
 .replace("'CropSpecies.ENDER_EYE.ordinal()!=118','CropSpecies.ENDER_EYE.ordinal()!=118||CropSpecies.OCHRE_FROGLIGHT.ordinal()!=130'","'CropSpecies.OCHRE_FROGLIGHT.ordinal()!=130','CropSpecies.OCHRE_FROGLIGHT.ordinal()!=130||CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()!=142'")
 .replace("'length != 131','length != 143'","'length != 143','length != 155'")
 .replace("'CropSpecies.OCHRE_FROGLIGHT','CropSpecies.PEARLESCENT_FROGLIGHT'","'CropSpecies.PEARLESCENT_FROGLIGHT','CropSpecies.WARPED_FUNGUS'")
 .replace("'== 33','== 36'","'== 36','== 39'")
 .replace("'thirty-three species','thirty-six species'","'thirty-six species','thirty-nine species'")
 .replace("'==128','==140'","'==140','==152'").replace("'<=128','<=140'","'<=140','<=152'")
 .replace("'all 126 crosses','all 138 crosses'","'all 138 crosses','all 150 crosses'")
 .replace("'==84','==92'","'==92','==100'").replace("'==42','==46'","'==46','==50'")
 .replace("'all 131 breeding','all 143 breeding'","'all 143 breeding','all 155 breeding'")
 .replaceAll('ResourceExpansionEight','ResourceExpansionNine').replaceAll('ResourceExpansionSeven','ResourceExpansionEight')
 .replaceAll('>=CropSpecies.CHARCOAL.ordinal()','>=CropSpecies.SPRUCE_LOG.ordinal()')
 .replaceAll('&&species.ordinal()<=CropSpecies.OCHRE_FROGLIGHT.ordinal()','&&species.ordinal()<=CropSpecies.PEARLESCENT_FROGLIGHT.ordinal()')
 .replace("'>=CropSpecies.SPRUCE_LOG.ordinal()','>=CropSpecies.SPRUCE_LOG.ordinal()'","'>=CropSpecies.SPRUCE_LOG.ordinal()','>=CropSpecies.SUGAR.ordinal()'")
 .replace('species==CropSpecies.DARK_PRISMARINE||species==CropSpecies.GILDED_BLACKSTONE||species==CropSpecies.SCULK_CATALYST||species==CropSpecies.OCHRE_FROGLIGHT','species==CropSpecies.CRIMSON_STEM||species==CropSpecies.WARPED_STEM||species==CropSpecies.VERDANT_FROGLIGHT||species==CropSpecies.PEARLESCENT_FROGLIGHT')
 .replace("'CropSpecies.CHARCOAL.ordinal()','CropSpecies.SPRUCE_LOG.ordinal()'","'CropSpecies.SPRUCE_LOG.ordinal()','CropSpecies.SUGAR.ordinal()'");
fs.writeFileSync('tools/finish-resource9.cjs',s);
console.log('All twelve breeding pairs are distinct from existing recipes.');

