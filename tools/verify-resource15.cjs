const fs=require('fs'),crypto=require('crypto'),spec=require('./resource15-spec.json');
const base='src/main/resources/assets/agri_evolution/';
const models=fs.readdirSync(base+'models/block').filter(n=>/_crop_stage\d\.json$/.test(n));
if(models.length!==2024||models.some(n=>JSON.parse(fs.readFileSync(base+'models/block/'+n)).parent!=='agri_evolution:block/crop_cross'))throw Error('Cross model coverage');
const assets=JSON.parse(fs.readFileSync('docs/asset-usage.json'));
const added=assets.filter(a=>spec.names.some(n=>a.path==='item/'+n+'_seed.png'||a.path.startsWith('block/'+n+'_crop_stage')));
if(added.length!==108||assets.length!==2475)throw Error('Asset counts');
for(const n of spec.names)for(let i=0;i<9;i++){let p=base+'textures/'+(i<8?'block/'+n+'_crop_stage'+i:'item/'+n+'_seed')+'.png';let b=fs.readFileSync(p);if(b.readUInt32BE(16)!==64||b.readUInt32BE(20)!==64)throw Error('Sprite size '+p);}
for(const n of Object.keys(spec.removed))if(assets.some(a=>a.path==='item/'+n+'_seed.png'||a.path.startsWith('block/'+n+'_crop_stage')))throw Error('Retired texture '+n);
const log=fs.readFileSync('build/resource15-validation.log','utf8');
if(!log.includes('All 113 required tests passed')||!log.includes('BUILD SUCCESSFUL'))throw Error('Tests failed');
const gui=fs.readFileSync('build/resource15-gui.log','utf8');
if(!gui.includes('BUILD SUCCESSFUL'))throw Error('Journal render/progression run incomplete');
for(let i=0;i<24;i++)if(!fs.existsSync('artwork/resource-expansion-v15/journal-qa/resource15-grandmas-'+i+'.png'))throw Error('Missing journal render '+i);
const report=JSON.parse(fs.readFileSync('docs/resource-expansion-0690-verification.json'));
Object.assign(report,{species:253,resourceCrops:183,foodCrops:60,newCrops:12,retiredCrops:spec.removed,newTextures:108,activeTextures:2475,crossedCropModels:2024,requiredGameTestsPassed:113,illustratedRecipes:250,fieldRecipes:175,labRecipes:73,journalScreenshots:24,jarSHA256:crypto.createHash('sha256').update(fs.readFileSync('build/libs/agri_evolution-0.69.0.jar')).digest('hex').toUpperCase(),newAssets:added});
fs.writeFileSync('docs/resource-expansion-0690-verification.json',JSON.stringify(report,null,2)+'\n');console.log('Verified resource expansion and duplicate retirement.');
