const fs=require('fs');
const {names,field,lab}=require('./resource8-spec.json');
let s=fs.readFileSync('tools/add-resource-expansion7.cjs','utf8');
s=s.replace(/const names=.*?;/,'const names='+JSON.stringify(names)+';')
 .replaceAll('resource-expansion-v7','resource-expansion-v8')
 .replace("includes('CHARCOAL(')","includes('SPRUCE_LOG(')")
 .replaceAll('"ender_eye_seed", true','"ochre_froglight_seed", true')
 .replaceAll('AEBlocks.ENDER_EYE_CROP','AEBlocks.OCHRE_FROGLIGHT_CROP')
 .replace(/const crosses=.*?;/,'const crosses='+JSON.stringify(field)+';')
 .replace(/const labCrosses=.*?;/,'const labCrosses='+JSON.stringify(lab)+';')
 .replace('["dark_prismarine","gilded_blackstone","sculk_catalyst","ochre_froglight"]',JSON.stringify(names.slice(8)))
 .replace("s.replace('mod_version=0.54.0','mod_version=0.55.0')","s.replace('mod_version=0.55.0','mod_version=0.56.0')")
 .replace('additive 0.55.0','additive 0.56.0');
fs.writeFileSync('tools/add-resource-expansion8.cjs',s);
s=fs.readFileSync('tools/finish-resource7.cjs','utf8')
 .replace('./resource7-spec.json','./resource8-spec.json')
 .replace(/const blocks=.*?;/,'const blocks='+JSON.stringify(['SPRUCE_LOG','BIRCH_LOG','ACACIA_LOG','DARK_OAK_LOG','MANGROVE_LOG','CHERRY_LOG','PRISMARINE','MOSS_BLOCK','CRIMSON_NYLIUM','WARPED_NYLIUM','VERDANT_FROGLIGHT','PEARLESCENT_FROGLIGHT'])+';')
 .replace('case CHARCOAL ->','case SPRUCE_LOG ->')
 .replace("'<= ENDER_EYE.ordinal()','<= OCHRE_FROGLIGHT.ordinal()'","'<= OCHRE_FROGLIGHT.ordinal()','<= PEARLESCENT_FROGLIGHT.ordinal()'")
 .replace("'count()!=85','count()!=97'","'count()!=97','count()!=109'")
 .replace("'CropSpecies.SCULK.ordinal()!=106','CropSpecies.SCULK.ordinal()!=106||CropSpecies.ENDER_EYE.ordinal()!=118'","'CropSpecies.ENDER_EYE.ordinal()!=118','CropSpecies.ENDER_EYE.ordinal()!=118||CropSpecies.OCHRE_FROGLIGHT.ordinal()!=130'")
 .replace("'length != 119','length != 131'","'length != 131','length != 143'")
 .replace("'CropSpecies.ENDER_EYE','CropSpecies.OCHRE_FROGLIGHT'","'CropSpecies.OCHRE_FROGLIGHT','CropSpecies.PEARLESCENT_FROGLIGHT'")
 .replace("'== 30','== 33'","'== 33','== 36'")
 .replace("'thirty species','thirty-three species'","'thirty-three species','thirty-six species'")
 .replace("'==116','==128'","'==128','==140'").replace("'<=116','<=128'","'<=128','<=140'")
 .replace("'all 114 crosses','all 126 crosses'","'all 126 crosses','all 138 crosses'")
 .replace("'==76','==84'","'==84','==92'").replace("'==38','==42'","'==42','==46'")
 .replace("'all 119 breeding','all 131 breeding'","'all 131 breeding','all 143 breeding'")
 .replaceAll('ResourceExpansionSeven','ResourceExpansionEight').replaceAll('ResourceExpansionSix','ResourceExpansionSeven')
 .replaceAll('>=CropSpecies.RED_SAND.ordinal()','>=CropSpecies.CHARCOAL.ordinal()')
 .replaceAll('&&species.ordinal()<=CropSpecies.ENDER_EYE.ordinal()','&&species.ordinal()<=CropSpecies.OCHRE_FROGLIGHT.ordinal()')
 .replace("'>=CropSpecies.CHARCOAL.ordinal()','>=CropSpecies.CHARCOAL.ordinal()'","'>=CropSpecies.CHARCOAL.ordinal()','>=CropSpecies.SPRUCE_LOG.ordinal()'")
 .replace('species==CropSpecies.BLUE_ICE||species==CropSpecies.MAGMA_BLOCK||species==CropSpecies.NETHER_BRICK||species==CropSpecies.ENDER_EYE','species==CropSpecies.DARK_PRISMARINE||species==CropSpecies.GILDED_BLACKSTONE||species==CropSpecies.SCULK_CATALYST||species==CropSpecies.OCHRE_FROGLIGHT')
 .replace("'CropSpecies.RED_SAND.ordinal()','CropSpecies.CHARCOAL.ordinal()'","'CropSpecies.CHARCOAL.ordinal()','CropSpecies.SPRUCE_LOG.ordinal()'");
fs.writeFileSync('tools/finish-resource8.cjs',s);
// Refuse conflicting parent pairs before editing gameplay sources.
const sources=['SpeciesBreeder','AdvancedHybridBreeder'].map(f=>fs.readFileSync('src/main/java/com/agrievolution/genetics/'+f+'.java','utf8')).join('\n');
const pairs=[...sources.matchAll(/pair\(first, second, CropSpecies\.(\w+), CropSpecies\.(\w+)\)/g)].map(m=>[m[1],m[2]].sort().join('/'));
for(const [a,b]of [...field,...lab])if(pairs.includes([a,b].sort().join('/')))throw Error('Duplicate parents '+a+' '+b);
console.log('All twelve new breeding pairs are distinct from existing recipes.');

