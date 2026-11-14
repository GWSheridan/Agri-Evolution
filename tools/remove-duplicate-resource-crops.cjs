// One-time 0.69 migration. The complete 0.68 source and JAR are archived first.
const fs=require('fs'),path=require('path'),spec=require('./resource15-spec.json');
const removed=Object.keys(spec.removed),ups=removed.map(n=>n.toUpperCase());
const root='src/main/java/com/agrievolution/';
const walk=p=>fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(p,e.name)):[path.join(p,e.name)]);
for(const file of walk(root).filter(p=>p.endsWith('.java'))){
 let s=fs.readFileSync(file,'utf8');
 if(file.endsWith('CropSpecies.java'))s=s.split('\n').filter(l=>!ups.some(n=>new RegExp('^\\s*'+n+'\\(').test(l))).join('\n');
 if(file.endsWith('AEItems.java')||file.endsWith('AEBlocks.java'))s=s.split('\n').filter(l=>!ups.some(n=>new RegExp('public static final .* '+n+'_(SEED|CROP) =').test(l))).join('\n');
 if(file.endsWith('AECreativeTabs.java'))s=s.split('\n').filter(l=>!ups.some(n=>l.includes('AEItems.'+n+'_SEED.get()'))).join('\n');
 if(file.endsWith('AEBlockEntities.java'))for(const n of ups)s=s.replaceAll(', AEBlocks.'+n+'_CROP.get()','');
 // Enum switch arms (including grouped journal labels); do not touch vanilla Blocks/Items.
 s=s.replace(/case ([A-Z_]+(?:,\s*[A-Z_]+)*)\s*->[^\n]+/g,(line,list)=>{let keep=list.split(/,\s*/).filter(n=>!ups.includes(n));return keep.length?line.replace(list,keep.join(', ')):'';});
 if(file.endsWith('SpeciesBreeder.java')||file.endsWith('AdvancedHybridBreeder.java')){
  s=s.replace(/        if \(pair\([^\n]+\)\)\r?\n\s*return new (?:MutationRecipe|HybridRecipe)\(CropSpecies\.([A-Z_]+),[^\n]+\r?\n/g,(line,n)=>ups.includes(n)?'':line);
  s=s.replaceAll('CropSpecies.MAGMA_BLOCK','CropSpecies.MAGMA_CREAM');
 }
 fs.writeFileSync(file,s);
}
// Keep the older batch regression suites, with only the retired rows removed.
for(const file of walk('src/test/java').filter(p=>p.endsWith('.java'))){let s=fs.readFileSync(file,'utf8');
 if(file.endsWith('ResourceProgressionSelfTest.java')){
  s=s.replace(/^.*(?:field|lab)\(CropSpecies\.[^\n]+,CropSpecies\.([A-Z_]+),\d+\);\r?\n/gm,(l,n)=>ups.includes(n)?'':l).replaceAll('CropSpecies.MAGMA_BLOCK','CropSpecies.MAGMA_CREAM');
 }
 const species=s.match(/var species=new CropSpecies\[\]\{([^}]+)\}/),blocks=s.match(/var blocks=new Block\[\]\{([^}]+)\}/),speeds=s.match(/float\[\] speeds=\{([^}]+)\}/);
 if(species&&blocks&&speeds){const rows=species[1].split(','),keep=rows.map((n,i)=>ups.includes(n.trim().replace('CropSpecies.',''))?-1:i).filter(i=>i>=0);
  if(keep.length!==rows.length){for(const m of [species,blocks,speeds]){const values=m[1].split(',');s=s.replace(m[0],m[0].replace(m[1],keep.map(i=>values[i]).join(',')));}s=s.replace('index==12','index=='+keep.length);}
 }
 fs.writeFileSync(file,s);
}
const assets='src/main/resources/assets/agri_evolution/';
for(const n of removed){const files=[assets+`blockstates/${n}_crop.json`,assets+`models/item/${n}_seed.json`,assets+`textures/item/${n}_seed.png`,'src/main/resources/data/agri_evolution/loot_table/blocks/'+n+'_crop.json',...Array.from({length:8},(_,i)=>assets+`models/block/${n}_crop_stage${i}.json`),...Array.from({length:8},(_,i)=>assets+`textures/block/${n}_crop_stage${i}.png`)];for(const f of files){const abs=path.resolve(f);if(!abs.startsWith(path.resolve('src')+path.sep))throw Error(abs);if(fs.existsSync(abs))fs.unlinkSync(abs);}}
const lang=JSON.parse(fs.readFileSync(assets+'lang/en_us.json'));for(const n of removed){delete lang['block.agri_evolution.'+n+'_crop'];delete lang['item.agri_evolution.'+n+'_seed'];}fs.writeFileSync(assets+'lang/en_us.json',JSON.stringify(lang,null,2)+'\n');
console.log('Removed ten duplicate species and their packaged assets.');
