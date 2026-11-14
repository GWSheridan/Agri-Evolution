const fs=require('fs');
const {names,field,lab}=require('./resource7-spec.json');
let migration=fs.readFileSync('tools/add-resource-expansion6.cjs','utf8');
migration=migration.replace(/const names=.*?;/,'const names='+JSON.stringify(names)+';')
 .replaceAll('resource-expansion-v6','resource-expansion-v7')
 .replace("includes('RED_SAND(')","includes('CHARCOAL(')")
 .replaceAll('"sculk_seed", true','"ender_eye_seed", true')
 .replaceAll('AEBlocks.SCULK_CROP','AEBlocks.ENDER_EYE_CROP')
 .replace(/const crosses=.*?;/,'const crosses='+JSON.stringify(field)+';')
 .replace(/const labCrosses=.*?;/,'const labCrosses='+JSON.stringify(lab)+';')
 .replace("['blue_ice','magma_block','nether_brick','ender_eye']",JSON.stringify(names.slice(8)))
 .replace("s.replace('mod_version=0.53.1','mod_version=0.54.0')","s.replace('mod_version=0.54.0','mod_version=0.55.0')")
 .replace('additive 0.54.0','additive 0.55.0');
fs.writeFileSync('tools/add-resource-expansion7.cjs',migration);

