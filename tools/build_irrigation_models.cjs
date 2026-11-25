// Irrigation fitting geometry. Material provenance is kept in the local artwork/irrigation-v2 archive.
const fs=require('fs');
const root='common/src/main/resources/assets/agri_evolution/';
const textures=Object.fromEntries(['copper','enamel','nozzle'].map(n=>[n,'agri_evolution:block/irrigation_'+n]));
textures.particle=textures.copper;
function box(from,to,material='copper',rotation) {
 const faces=Object.fromEntries(['north','south','east','west','up','down'].map(d=>[d,{texture:'#'+material}]));
 return {from,to,faces,...(rotation?{rotation}:{})};
}
function save(path,obj){fs.writeFileSync(root+path,JSON.stringify(obj,null,2)+'\n');}
function model(name,elements){save('models/block/'+name+'.json',{parent:'minecraft:block/block',textures,elements});}
for(const closed of [false,true]) {
 const e=[box([6,6,2],[10,10,14]),box([5,5,0],[11,11,2],'enamel'),box([5,5,14],[11,11,16],'enamel'),box([5,5,5],[11,11,11],'enamel'),box([7,10,7],[9,14,9])];
 e.push(box([5,13,3],[11,15,4.5]),box([5,13,11.5],[11,15,13]),box([3,13,5],[4.5,15,11]),box([11.5,13,5],[13,15,11]));
 for(const [x,z,a] of [[4.6,4.6,-45],[11.4,11.4,-45],[4.6,11.4,45],[11.4,4.6,45]]) e.push(box([x-1.6,13,z-.75],[x+1.6,15,z+.75],'copper',{origin:[x,14,z],axis:'y',angle:a}));
 const rot=closed?{origin:[8,14,8],axis:'y',angle:45}:undefined;
 e.push(box([7.4,13.4,4],[8.6,14.6,12],'copper',rot),box([4,13.4,7.4],[12,14.6,8.6],'copper',rot),box([7,14.6,7],[9,15,9],closed?'copper':'enamel'));
 model('irrigation_valve_'+(closed?'closed':'open'),e);
}
const rotations={north:{},east:{y:90},south:{y:180},west:{y:270},up:{x:270},down:{x:90}};
const valveParts=[];
for(const [f,r] of Object.entries(rotations))for(const open of [false,true])for(const powered of [false,true])valveParts.push({when:{facing:f,open:String(open),powered:String(powered)},apply:{model:'agri_evolution:block/irrigation_valve_'+(open&&!powered?'open':'closed'),...r}});
const axis=d=>['up','down'].includes(d)?'y':['east','west'].includes(d)?'x':'z';
for(const [d,r] of Object.entries(rotations))valveParts.push({when:{[d]:'true',facing:Object.keys(rotations).filter(f=>axis(f)!==axis(d)).join('|')},apply:{model:'agri_evolution:block/irrigation_pipe_arm',...r}});
save('blockstates/irrigation_valve.json',{multipart:valveParts});
// Pieces meet at edges without coplanar copper/enamel outer faces.
const e=[box([3,.25,6],[13,2.1,10],'enamel'),box([6,.25,3],[10,2.1,6],'enamel'),box([6,.25,10],[10,2.1,13],'enamel'),box([6.5,2.1,6.5],[9.5,10,9.5]),box([6,10,6],[10,13,10],'enamel'),box([7,11,3],[9,13,6]),box([7,11,10],[9,13,13]),box([3,11,7],[6,13,9]),box([10,11,7],[13,13,9])];
for(const [x,z] of [[8,2],[8,14],[2,8],[14,8]]) {const tip=box([x-1,12,z-1],[x+1,15,z+1]);tip.faces.up={texture:'#nozzle',uv:[0,0,16,16]};e.push(tip);}
// Hang from the ceiling: mounting foot above, nozzle openings facing crops below.
for(const part of e) {
 const bottom=part.from[1];part.from[1]=16-part.to[1];part.to[1]=16-bottom;
 [part.faces.up,part.faces.down]=[part.faces.down,part.faces.up];
}
model('irrigation_sprinkler',e);
model('irrigation_sprinkler_connector',[box([6.25,6.25,2],[9.75,9.75,8]),box([5,5,0],[11,11,2],'enamel')]);
save('blockstates/irrigation_sprinkler.json',{multipart:[{apply:{model:'agri_evolution:block/irrigation_sprinkler'}},...Object.entries(rotations).map(([d,r])=>({when:{[d]:'true'},apply:{model:'agri_evolution:block/irrigation_sprinkler_connector',...r}}))]});

