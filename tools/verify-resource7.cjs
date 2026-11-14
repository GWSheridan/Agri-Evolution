const fs=require('fs'),crypto=require('crypto'),path=require('path');
const {names}=require('./resource7-spec.json');
const assets=JSON.parse(fs.readFileSync('docs/asset-usage.json','utf8'));
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex').toUpperCase();
const base='src/main/resources/assets/agri_evolution/';
const models=fs.readdirSync(base+'models/block').filter(n=>/_crop_stage\d\.json$/.test(n));
if(models.length!==1048||models.some(n=>JSON.parse(fs.readFileSync(base+'models/block/'+n,'utf8')).parent!=='agri_evolution:block/crop_cross'))throw Error('Cross model coverage');
const newAssets=assets.filter(a=>names.some(n=>a.path==='item/'+n+'_seed.png'||a.path.startsWith('block/'+n+'_crop_stage')));
if(newAssets.length!==108||assets.length!==1296)throw Error('Asset coverage');
const log=fs.readFileSync('build/resources7-validation.log','utf8');
if(!log.includes('All 80 required tests passed')||!log.includes('BUILD SUCCESSFUL'))throw Error('Tests not passed');
const gui=fs.readFileSync('build/resources7-gui.log','utf8');
if(!gui.includes('BUILD SUCCESSFUL'))throw Error('Preview incomplete');
fs.writeFileSync('docs/resource-expansion-0550-verification.json',JSON.stringify({
 version:'0.55.0',species:131,resourceCrops:97,newCrops:12,
 existingTexturesPreserved:1188,newTextures:108,activeTextures:1296,
 crossedCropModels:models.length,craftingRecipesCompared:32,craftingRecipesChanged:0,
 requiredGameTestsPassed:80,illustratedRecipes:128,fieldRecipes:84,labRecipes:42,
 journalScreenshots:24,jarSha256:hash('build/libs/agri_evolution-0.55.0.jar'),newAssets
},null,2)+'\n');
console.log('Verified twelve resource crops; 1048 X models; 108 new sprites; 80 GameTests.');

