// Reference inventory, not proof of copyright ownership.
const fs = require('node:fs'), path = require('node:path'), crypto = require('node:crypto');
const base = 'src/main/resources/assets/agri_evolution';
function walk(dir) { return fs.existsSync(dir) ? fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(`${dir}/${e.name}`):[`${dir}/${e.name}`]) : []; }
const refs = new Set();
for (const f of walk(base+'/models').filter(f=>f.endsWith('.json'))) {
  const model=JSON.parse(fs.readFileSync(f,'utf8').replace(/^\uFEFF/,''));
  for (const ref of Object.values(model.textures||{})) if (ref.startsWith('agri_evolution:')) refs.add(ref.slice(15)+'.png');
}
const java=walk('src/main/java').filter(f=>f.endsWith('.java')).map(f=>fs.readFileSync(f,'utf8')).join('\n');
// Dynamic paths reviewed in JournalTheme, JournalRenderer, CompactJournalRenderer
// and GardenGuiTheme. Discovered portraits use current crop textures; tabs are panels.
const runtime = new Set([
  'gui/journal/journal_background.png', 'gui/journal/journal_background_compact.png',
  'gui/journal/arrow_left.png', 'gui/journal/arrow_right.png',
  'gui/journal/species/undiscovered.png', 'gui/journal/species/small/undiscovered.png',
  'gui/garden/garden_frame.png', 'gui/garden/garden_header.png', 'gui/garden/seed_tray_slot.png'
]);
for (const name of [...refs, ...runtime]) {
  if (!fs.existsSync(base+'/textures/'+name)) throw Error('Missing referenced texture: '+name);
}
const files=walk(base+'/textures').filter(f=>f.endsWith('.png'));
const rows=files.map(file=>{
  const name=file.slice((base+'/textures/').length);
  const usage=refs.has(name)?'model':runtime.has(name)?'runtime-gui':java.includes('textures/'+name)?'java':'no-reference-found';
  const legacy=/^(plant|seed)\//.test(name);
  if(legacy&&usage!=='no-reference-found') throw Error('Legacy exclusion would break reference: '+name);
  return {path:name,sha256:crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex'),usage,
    status:legacy?'legacy-import-excluded': 'retained-review-or-approved'};
});
const reportDirectory = 'build/audits';
fs.mkdirSync(reportDirectory,{recursive:true});
fs.writeFileSync(reportDirectory+'/asset-usage.json',JSON.stringify(rows,null,2)+'\n');
const totals={pngs:rows.length,excludedLegacy:rows.filter(r=>r.status==='legacy-import-excluded').length};
for(const r of rows) totals[r.usage]=(totals[r.usage]||0)+1;
console.log(JSON.stringify(totals));
