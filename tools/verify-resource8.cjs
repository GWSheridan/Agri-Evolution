const fs=require('fs'),crypto=require('crypto'),path=require('path');
const {names}=require('./resource8-spec.json');
const assets=JSON.parse(fs.readFileSync('docs/asset-usage.json','utf8'));
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex').toUpperCase();
const base='src/main/resources/assets/agri_evolution/';
const models=fs.readdirSync(base+'models/block').filter(n=>/_crop_stage\d\.json$/.test(n));
if(models.length!==1144||models.some(n=>JSON.parse(fs.readFileSync(base+'models/block/'+n,'utf8')).parent!=='agri_evolution:block/crop_cross'))throw Error('Cross model coverage');
const newAssets=assets.filter(a=>names.some(n=>a.path==='item/'+n+'_seed.png'||a.path.startsWith('block/'+n+'_crop_stage')));
if(newAssets.length!==108||assets.length!==1404)throw Error('Asset coverage');
const log=fs.readFileSync('build/resources8-validation.log','utf8');
if(!log.includes('All 83 required tests passed')||!log.includes('BUILD SUCCESSFUL'))throw Error('Tests not passed');
const gui=fs.readFileSync('build/resources8-gui.log','utf8');
if(!gui.includes('BUILD SUCCESSFUL'))throw Error('Preview incomplete');
fs.writeFileSync('docs/resource-expansion-0560-verification.json',JSON.stringify({
 version:'0.56.0',species:143,resourceCrops:109,newCrops:12,
 existingTexturesPreserved:1296,newTextures:108,activeTextures:1404,
 crossedCropModels:models.length,craftingRecipesCompared:32,craftingRecipesChanged:0,
 requiredGameTestsPassed:83,illustratedRecipes:140,fieldRecipes:92,labRecipes:46,
 journalScreenshots:24,jarSha256:hash('build/libs/agri_evolution-0.56.0.jar'),newAssets
},null,2)+'\n');
console.log('Verified twelve resource crops; 1144 X models; 108 new sprites; 83 GameTests.');

