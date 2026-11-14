// Compare against extracted, unmodified Minecraft 1.21.1 recipe/item-tag JSON.
// Usage: node dev_checks/recipe_conflicts.cjs <vanilla-data.json>
const fs = require('node:fs');
const root = 'src/main/resources/data/agri_evolution/recipe';
const data = Object.fromEntries(Object.entries(JSON.parse(fs.readFileSync(process.argv[2], 'utf8').replace(/^\uFEFF/, ''))).map(([k,v]) => [k, typeof v === 'string' ? JSON.parse(v) : v]));
function tag(id, seen = new Set()) {
  if (seen.has(id)) throw Error('Tag cycle: ' + id);
  const [ns, path] = id.split(':');
  const value = data[`data/${ns}/tags/item/${path}.json`];
  if (!value) throw Error('Unresolved tag: ' + id);
  return value.values.flatMap(v => {
    v = typeof v === 'string' ? v : v.id;
    return v.startsWith('#') ? tag(v.slice(1), new Set([...seen, id])) : [v];
  });
}
function ingredient(i) {
  if (Array.isArray(i)) return new Set(i.flatMap(v => [...ingredient(v)]));
  return new Set(i.item ? [i.item] : tag(i.tag));
}
function overlaps(a,b) { return a === null || b === null ? a === b : [...a].some(v => b.has(v)); }
function shaped(r) {
  let rows = [...r.pattern];
  while (!rows[0].trim()) rows.shift();
  while (!rows.at(-1).trim()) rows.pop();
  const left = Math.min(...rows.map(r => r.search(/\S/)).filter(n => n >= 0));
  const right = Math.max(...rows.map(r => r.trimEnd().length));
  rows = rows.map(r => r.slice(left,right));
  const grids=[];
  for (const mirror of [false,true]) for (let y=0;y<=3-rows.length;y++) for(let x=0;x<=3-rows[0].length;x++) {
    const grid=Array(9).fill(null);
    rows.forEach((row,j) => [...(mirror ? [...row].reverse().join('') : row)].forEach((c,i) => { if(c !== ' ') grid[(y+j)*3+x+i]=ingredient(r.key[c]); }));
    grids.push(grid);
  }
  return grids;
}
function matchLists(a,b) {
  if(a.length !== b.length) return false;
  if(!a.length) return true;
  return b.some((v,i) => overlaps(a[0],v) && matchLists(a.slice(1),b.filter((_,j)=>j!==i)));
}
const standard = r => ['minecraft:crafting_shaped','minecraft:crafting_shapeless'].includes(r.type);
function prepare(r) { return r.type.endsWith('_shaped') ? {grids:shaped(r)} : {list:r.ingredients.map(ingredient)}; }
function conflict(a,b) {
  if(a.grids && b.grids) return a.grids.some(x=>b.grids.some(y=>x.every((v,i)=>overlaps(v,y[i]))));
  return matchLists(a.list || a.grids[0].filter(Boolean), b.list || b.grids[0].filter(Boolean));
}
const vanilla=Object.entries(data).filter(([k,r])=>k.includes('/recipe/') && standard(r)).map(([k,r])=>[k,prepare(r)]);
const mods=fs.readdirSync(root).filter(n=>n.endsWith('.json')).map(n=>[n,JSON.parse(fs.readFileSync(`${root}/${n}`,'utf8').replace(/^\uFEFF/,''))]).filter(([n,r])=>standard(r)).map(([n,r])=>[n,prepare(r)]);
let failures=0;
for(const [name,r] of mods) for(const [other,v] of vanilla) if(conflict(r,v)) {console.log(`CONFLICT: ${name} / ${other}`);failures++;}
for(let i=0;i<mods.length;i++) for(let j=i+1;j<mods.length;j++) if(conflict(mods[i][1],mods[j][1])) {console.log(`INTERNAL CONFLICT: ${mods[i][0]} / ${mods[j][0]}`);failures++;}
console.log(`Checked ${mods.length} mod recipes against ${vanilla.length} vanilla shaped/shapeless recipes, including tags, mirroring, offsets and internal overlaps: ${failures} conflicts.`);
console.log('Special serializers requiring separate review:', [...new Set(Object.values(data).filter(r=>r.type?.includes('crafting_special')).map(r=>r.type))].join(', '));
process.exitCode=failures?1:0;
