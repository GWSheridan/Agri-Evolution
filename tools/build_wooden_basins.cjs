// Hollow trough geometry; material artwork is kept separately in the local archive.
const fs=require('node:fs');
const root='common/src/main/resources/assets/agri_evolution/models/block/';
function box(from,to,texture,uv){
  return {from,to,faces:Object.fromEntries(['north','south','east','west','up','down'].map(side=>[side,{texture:'#'+texture,...(uv?{uv}:{})}]))};
}
// Preserve a uniform 4 texels per model unit on EVERY decorative face.
// Square artwork cannot be assigned wholesale to a long rail or a wafer-thin cap edge.
function fittedBox(from,to,texture,band=false){
  const [dx,dy,dz]=to.map((v,i)=>v-from[i]);
  const sizes={north:[dx,dy],south:[dx,dy],east:[dz,dy],west:[dz,dy],up:[dx,dz],down:[dx,dz]};
  const faces={};
  for(const [side,[w,h]] of Object.entries(sizes)){
    const u=(16-w)/2;
    // Bands sample a narrow horizontal strip; caps and plaques sample the central motif.
    const v=band?(texture==='copper'?1.15:1.5):(16-h)/2;
    faces[side]={texture:'#'+texture,uv:[u,v,u+w,v+h]};
  }
  return {from,to,faces};
}
for(let tier=0;tier<3;tier++)for(const filled of [false,true]){
  const trim=tier===0?'wood':tier===1?'copper':'amethyst';
  const elements=[box([1,0,1],[15,2,15],'wood'),
    box([1,2,1],[3,11,15],'wood',[0,0,16,12]),box([13,2,1],[15,11,15],'wood',[0,0,16,12]),
    box([3,2,1],[13,11,3],'wood',[0,0,16,12]),box([3,2,13],[13,11,15],'wood',[0,0,16,12])];
  // Raised narrow rails frame the timber without covering its grain. No coplanar faces.
  for(const y of [2.5,9.5]){
    const h=tier===0?.65:1;
    const rail=(a,b)=>tier===0?box(a,b,trim,[0,0,16,3]):fittedBox(a,b,trim,true);
    elements.push(rail([.85,y,.85],[15.15,y+h,1.05]),
      rail([.85,y,14.95],[15.15,y+h,15.15]),
      rail([.85,y,1.05],[1.05,y+h,14.95]),
      rail([14.95,y,1.05],[15.15,y+h,14.95]));
  }
  // Flush metal / jewel caps on the four corners, and a decorative medallion on each side.
  if(tier>0){
    for(const x of [1,13])for(const z of [1,13])elements.push(fittedBox([x-.05,10.95,z-.05],[x+2.05,11.15,z+2.05],trim));
    elements.push(fittedBox([6,4,.8],[10,8,1.05],trim),fittedBox([6,4,14.95],[10,8,15.2],trim),
      fittedBox([.8,4,6],[1.05,8,10],trim),fittedBox([14.95,4,6],[15.2,8,10],trim));
  }
  if(filled)elements.push({from:[3,9.25,3],to:[13,9.25,13],shade:false,
    faces:{up:{texture:'#water',uv:[3,3,13,13],tintindex:0}}});
  const textures={particle:'agri_evolution:block/basin_wood',wood:'agri_evolution:block/basin_wood',copper:'agri_evolution:block/basin_copper',amethyst:'agri_evolution:block/basin_amethyst'};
  if(filled)textures.water='minecraft:block/water_still';
  const model={parent:'minecraft:block/block',render_type:'minecraft:cutout',ambientocclusion:true,textures,elements};
  fs.writeFileSync(root+'irrigation_basin_'+(filled?'full':'empty')+(tier?'_mk'+(tier+1):'')+'.json',JSON.stringify(model,null,2)+'\n');
}
console.log('Six wooden basin states generated; only filled variants have water.');
