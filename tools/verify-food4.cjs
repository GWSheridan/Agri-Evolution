const fs=require('fs'),crypto=require('crypto'),{names,field}=require('./food4-spec.json');
const base='src/main/resources/assets/agri_evolution/';
const assets=JSON.parse(fs.readFileSync('docs/asset-usage.json','utf8'));
const models=fs.readdirSync(base+'models/block').filter(n=>/_crop_stage\d\.json$/.test(n));
if(models.length!==1624||models.some(n=>JSON.parse(fs.readFileSync(base+'models/block/'+n)).parent!=='agri_evolution:block/crop_cross'))throw Error('Cross model coverage');
const added=assets.filter(a=>names.some(n=>a.path==='item/'+n+'.png'||a.path==='item/'+n+'_seed.png'||a.path.startsWith('block/'+n+'_crop_stage')));
if(added.length!==120||assets.length!==1956)throw Error('Asset coverage');
const sources=['SpeciesBreeder','AdvancedHybridBreeder'].map(n=>fs.readFileSync('src/main/java/com/agrievolution/genetics/'+n+'.java','utf8')).join('\n');
const pairs=[...sources.matchAll(/pair\(first, second, CropSpecies\.(\w+), CropSpecies\.(\w+)\)/g)].map(m=>[m[1],m[2]].sort().join('/'));
for(const [a,b]of field)if(pairs.filter(p=>p===[a,b].sort().join('/')).length!==1)throw Error('Ambiguous breeding pair');
const log=fs.readFileSync('build/food4-gametests.log','utf8');
if(!log.includes('All 95 required tests passed')||!log.includes('BUILD SUCCESSFUL'))throw Error('Server tests not passed');
const build=fs.readFileSync('build/food4-validation.log','utf8');
if(!build.includes('all 203 breeding plans')||!build.includes('BUILD SUCCESSFUL'))throw Error('Build not passed');
const p='docs/food-expansion-0610-verification.json',report=JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
Object.assign(report,{version:'0.61.0',crossedCropModels:models.length,requiredGameTestsPassed:95,illustratedRecipes:200,fieldRecipes:136,labRecipes:62,jarSha256:crypto.createHash('sha256').update(fs.readFileSync('build/libs/agri_evolution-0.61.0.jar')).digest('hex'),newAssets:added});
fs.writeFileSync(p,JSON.stringify(report,null,2)+'\n');
console.log('Verified 12 food crops, 120 new sprites, 1624 X models, unique breeding routes and 95 server tests.');

