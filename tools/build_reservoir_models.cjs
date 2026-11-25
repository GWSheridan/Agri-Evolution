const fs=require('fs');
const root='common/src/main/resources/assets/agri_evolution/';
const textures={copper:'agri_evolution:block/irrigation_copper',enamel:'agri_evolution:block/irrigation_enamel',panel:'agri_evolution:block/reservoir_panel',particle:'agri_evolution:block/irrigation_copper'};
function box(from,to,t='copper'){return {from,to,faces:Object.fromEntries(['north','south','east','west','up','down'].map(d=>[d,{texture:'#'+t}]))};}
function save(p,o){fs.writeFileSync(root+p,JSON.stringify(o,null,2)+'\n');}
function model(n,e){save('models/block/'+n+'.json',{parent:'minecraft:block/block',textures,elements:e});}
model('irrigation_pipe',[box([5,5,5],[11,11,11],'enamel')]);
model('irrigation_pipe_arm',[box([6,6,2],[10,10,5]),box([5,5,0],[11,11,2],'enamel')]);
const dirs={north:{},east:{y:90},south:{y:180},west:{y:270},up:{x:270},down:{x:90}};
save('blockstates/irrigation_pipe.json',{multipart:[{apply:{model:'agri_evolution:block/irrigation_pipe'}},...Object.entries(dirs).map(([d,r])=>({when:{[d]:'true'},apply:{model:'agri_evolution:block/irrigation_pipe_arm',...r}}))]});
save('models/item/irrigation_pipe.json',{parent:'agri_evolution:block/irrigation_pipe_inventory'});
model('irrigation_pipe_inventory',[box([6,6,2],[10,10,14]),box([5,5,0],[11,11,2],'enamel'),box([5,5,14],[11,11,16],'enamel')]);
const all=[];
for(const [z0,z1,x0,x1] of [[4,8,16,32],[8,12,10,38],[12,16,6,42],[16,32,4,44],[32,36,6,42],[36,40,10,38],[40,44,16,32]]) {
 all.push(box([x0,4,z0],[x1,44,z1]));
 for(const y of [7,37])all.push(box([x0-.3,y,z0-.3],[x1+.3,y+3,z1+.3],'enamel'));
}
all.push(box([12,44,12],[36,46,36],'enamel'),box([19,46,19],[29,47.5,29]));
for(const x of [10,34])for(const z of [10,34])all.push(box([x,0,z],[x+4,5,z+4],'enamel'));
// Six real port collars terminate on the multiblock boundary.
for(const a of [0,1,2])for(const end of [0,44]) {const from=[21,21,21],to=[27,27,27];from[a]=end;to[a]=end+4;all.push(box(from,to,'enamel'));}
const plate=box([23,30,3],[32,41,4]);plate.faces.north={texture:'#panel',uv:[0,0,16,16]};all.push(plate);
// Gauge backing and four level segments; the same indicator is visible on the north face.
all.push(box([18,12,3],[21,32,4],'enamel'));
function clipped(elements,x,y,z) {return elements.flatMap(e=>{
 const lo=[x*16,y*16,z*16],hi=lo.map(v=>v+16),a=e.from.map((v,i)=>Math.max(v,lo[i])),b=e.to.map((v,i)=>Math.min(v,hi[i]));
 if(a.some((v,i)=>v>=b[i]))return [];
 const faces=structuredClone(e.faces);
 if(faces.north.uv)faces.north.uv=[(a[0]-e.from[0])/(e.to[0]-e.from[0])*16,(e.to[1]-b[1])/(e.to[1]-e.from[1])*16,(b[0]-e.from[0])/(e.to[0]-e.from[0])*16,(e.to[1]-a[1])/(e.to[1]-e.from[1])*16];
 return [{...e,faces,from:a.map((v,i)=>v-lo[i]),to:b.map((v,i)=>v-lo[i])}];
});}
const parts=[];
for(let x=0;x<3;x++)for(let y=0;y<3;y++)for(let z=0;z<3;z++) {
 const n=`water_reservoir_${x}${y}${z}`;model(n,clipped(all,x,y,z));
 parts.push({when:{part_x:String(x),part_y:String(y),part_z:String(z)},apply:{model:'agri_evolution:block/'+n}});
}
for(let fill=1;fill<=4;fill++) {
 const bars=[];for(let i=0;i<fill;i++)bars.push(box([18.4,13+i*4.5,2.8],[20.6,16.5+i*4.5,3],'copper'));
 for(let y=0;y<3;y++) {const e=clipped(bars,1,y,0);if(!e.length)continue;const n=`water_reservoir_gauge_${fill}_${y}`;model(n,e);parts.push({when:{part_x:'1',part_y:String(y),part_z:'0',fill:String(fill)},apply:{model:'agri_evolution:block/'+n}});}
}
save('blockstates/water_reservoir.json',{multipart:parts});

