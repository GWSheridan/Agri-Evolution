const fs=require('fs');const base='src/main/resources/assets/agri_evolution/';
const faces=t=>Object.fromEntries(['north','south','east','west','up','down'].map(d=>[d,{texture:'#'+t}]));
const box=(a,b,t='material')=>({from:a,to:b,faces:faces(t)});
for(const name of ['iron','obsidian']){
 const e=[];for(const x of [2,13])for(const z of [2,13])e.push(box([x,0,z],[x+1,14,z+1]));
 for(const y of name==='iron'?[5,11]:[4,8,12]){
  for(const z of [2,13])e.push(box([3,y,z],[13,y+1,z+1]));
  for(const x of [2,13])e.push(box([x,y,3],[x+1,y+1,13]));
 }
 for(const x of [1.75,12.75])for(const z of [1.75,12.75])e.push(box([x,12.75,z],[x+1.5,14.25,z+1.5],'trim'));
 const model={parent:'minecraft:block/block',textures:{material:'agri_evolution:block/'+name+'_support_material',trim:'agri_evolution:block/irrigation_copper',particle:'agri_evolution:block/'+name+'_support_material'},elements:e};
 fs.writeFileSync(base+'models/block/'+name+'_crop_support_overlay.json',JSON.stringify(model,null,2));
 fs.writeFileSync(base+'models/item/'+name+'_crop_support.json',JSON.stringify({parent:'agri_evolution:block/'+name+'_crop_support_overlay',display:{gui:{rotation:[30,225,0],translation:[0,0,0],scale:[.75,.75,.75]},ground:{scale:[.4,.4,.4]},fixed:{scale:[.7,.7,.7]}}},null,2));
}
for(const file of fs.readdirSync(base+'blockstates')){
 const path=base+'blockstates/'+file,j=JSON.parse(fs.readFileSync(path,'utf8'));
 if(!j.multipart?.some(p=>p.apply?.model==='agri_evolution:block/crop_support_overlay'))continue;
 j.multipart=j.multipart.filter(p=>!p.apply?.model?.endsWith('crop_support_overlay'));
 j.multipart.push({when:{supported:'true',support_tier:'0|1'},apply:{model:'agri_evolution:block/crop_support_overlay'}},{when:{supported:'true',support_tier:'2'},apply:{model:'agri_evolution:block/iron_crop_support_overlay'}},{when:{supported:'true',support_tier:'3'},apply:{model:'agri_evolution:block/obsidian_crop_support_overlay'}});
 fs.writeFileSync(path,JSON.stringify(j,null,2));
}
for(const [name,inputs] of [['iron',['agri_evolution:crop_support','minecraft:iron_ingot','minecraft:iron_ingot']],['obsidian',['agri_evolution:iron_crop_support','minecraft:obsidian','minecraft:obsidian','minecraft:amethyst_shard']]])fs.writeFileSync('src/main/resources/data/agri_evolution/recipe/'+name+'_crop_support.json',JSON.stringify({type:'minecraft:crafting_shapeless',category:'misc',ingredients:inputs.map(item=>({item})),result:{id:'agri_evolution:'+name+'_crop_support',count:1}},null,2));
const lang=base+'lang/en_us.json',j=JSON.parse(fs.readFileSync(lang,'utf8'));j['item.agri_evolution.iron_crop_support']='Iron Crop Support';j['item.agri_evolution.obsidian_crop_support']='Obsidian Crop Support';fs.writeFileSync(lang,JSON.stringify(j,null,2));
