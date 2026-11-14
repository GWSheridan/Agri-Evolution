// One-time, additive 0.49.0 content migration. Original art lives in artwork/resource-expansion-v1.
const fs=require('fs'),path=require('path');
const names=['string','wool','leather','bone','amethyst','prismarine','ghast_tear','breeze_rod'];
const java='src/main/java/com/agrievolution/',res='src/main/resources/';
if(fs.readFileSync(java+'genetics/CropSpecies.java','utf8').includes('STRING('))throw new Error('Resource crop migration already applied; use prepare-sprites.ps1 only to rebuild its assets.');
const edit=(p,fn)=>fs.writeFileSync(p,fn(fs.readFileSync(p,'utf8')));
const title=n=>n.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' '),up=n=>n.toUpperCase();
edit(java+'genetics/CropSpecies.java',s=>s.replace('"turmeric_seed", true);','"turmeric_seed", true),\n'+names.map(n=>`    ${up(n)}("${n}", "${title(n)}", "${n}_crop", "${n}_seed", true)`).join(',\n')+';'));
edit(java+'content/AEItems.java',s=>s.replace('    public static final DeferredItem<Item> BREEDING_JOURNAL',names.map(n=>`    public static final DeferredItem<Item> ${up(n)}_SEED = ITEMS.registerItem("${n}_seed", props -> new SpeciesSeedItem(props, CropSpecies.${up(n)}, AEBlocks.${up(n)}_CROP::get), new Item.Properties());`).join('\n')+'\n\n    public static final DeferredItem<Item> BREEDING_JOURNAL').replace('case NETHERITE -> NETHERITE_SEED.get();','case NETHERITE -> NETHERITE_SEED.get();\n'+names.map(n=>`            case ${up(n)} -> ${up(n)}_SEED.get();`).join('\n')));
edit(java+'content/AEBlocks.java',s=>s.replace('    public static final DeferredBlock<AgriCropBlock> NETHERITE_CROP',names.map(n=>`    public static final DeferredBlock<AgriCropBlock> ${up(n)}_CROP = BLOCKS.register("${n}_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.${up(n)}, AEItems.${up(n)}_SEED::get));`).join('\n')+'\n    public static final DeferredBlock<AgriCropBlock> NETHERITE_CROP'));
edit(java+'content/AEBlockEntities.java',s=>s.replace('AEBlocks.TURMERIC_CROP.get()).build', 'AEBlocks.TURMERIC_CROP.get(), '+names.map(n=>`AEBlocks.${up(n)}_CROP.get()`).join(', ')+').build'));
edit(java+'content/AECreativeTabs.java',s=>s.replace('output.accept(AEItems.BREEDING_JOURNAL.get());',names.map(n=>`output.accept(AEItems.${up(n)}_SEED.get());`).join('\n                        ')+'\n                        output.accept(AEItems.BREEDING_JOURNAL.get());'));
for(const file of ['content/AgriCropBlock.java','content/BotanicalGreenhouseBlockEntity.java'])edit(java+file,s=>s.replace('case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produce(species);', 'case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produce(species);\n'+names.map(n=>`            case ${up(n)} -> com.agrievolution.farming.ResourceCropRules.produce(species);`).join('\n')).replace('case NETHERITE -> AEBlocks.NETHERITE_CROP.get();','case NETHERITE -> AEBlocks.NETHERITE_CROP.get();\n'+names.map(n=>`                    case ${up(n)} -> AEBlocks.${up(n)}_CROP.get();`).join('\n')));
for(const [file,v] of [['CompactJournalRenderer.java','plant'],['JournalScreen.java','species']])edit(java+'client/journal/'+file,s=>s.replace(`case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produceName(${v});`,`case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produceName(${v});\n            case ${names.map(up).join(', ')} -> com.agrievolution.farming.ResourceCropRules.produceName(${v});`));
const crosses=[['WOOD','AZURE_FLAX','STRING',28],['STRING','WILD','WOOL',25],['WOOL','WOOD','LEATHER',22],['STONE','WILD','BONE',22],['CLAY','LAPIS','PRISMARINE',18]];
const labCrosses=[['QUARTZ','LAPIS','AMETHYST',14],['SOUL_SAND','ENDER_PEARL','GHAST_TEAR',8],['BLAZE_ROD','AMETHYST','BREEZE_ROD',8]];
edit(java+'genetics/AdvancedHybridBreeder.java',s=>s.replace('        return null;',labCrosses.map(([a,b,c,p])=>`        if (pair(first, second, CropSpecies.${a}, CropSpecies.${b}))\n            return new HybridRecipe(CropSpecies.${c}, ${p});`).join('\n')+'\n        return null;'));
edit(java+'genetics/SpeciesBreeder.java',s=>s.replace('        return null;',crosses.map(([a,b,c,p])=>`        if (pair(first, second, CropSpecies.${a}, CropSpecies.${b}))\n            return new MutationRecipe(CropSpecies.${c}, ${p});`).join('\n')+'\n        return null;'));
const write=(p,v)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');};
const assets=res+'assets/agri_evolution/';
for(const n of names){
 for(const p of ['blockstates/crimson_clover_crop.json',...Array.from({length:8},(_,i)=>`models/block/crimson_clover_crop_stage${i}.json`),'models/item/crimson_clover_seed.json']){
  fs.writeFileSync(assets+p.replaceAll('crimson_clover',n),fs.readFileSync(assets+p,'utf8').replaceAll('crimson_clover',n));
 }
 const loot=JSON.parse(fs.readFileSync(res+'data/agri_evolution/loot_table/blocks/crimson_clover_crop.json','utf8').replaceAll('crimson_clover',n));
 loot.pools[0].entries[0].name='minecraft:'+({wool:'white_wool',amethyst:'amethyst_shard',prismarine:'prismarine_shard'}[n]||n);
 if(['ghast_tear','breeze_rod'].includes(n))loot.pools[0].entries[0].functions=[];
 // Mature player harvest supplies the genetic seed in AgriCropBlock.playerDestroy.
 loot.pools[1].entries[0].conditions=[{condition:'minecraft:inverted',term:loot.pools[1].entries[0].conditions[0]}];
 write(res+`data/agri_evolution/loot_table/blocks/${n}_crop.json`,loot);
 for(let i=0;i<10;i++){if(i===8)continue;const id=n+(i<8?'_crop_stage'+i:i===8?'':'_seed');fs.copyFileSync(`artwork/resource-expansion-v1/sprites/${id}.png`,assets+`textures/${i<8?'block':'item'}/${id}.png`);}
}
edit(assets+'lang/en_us.json',s=>{const j=JSON.parse(s);for(const n of names){j[`block.agri_evolution.${n}_crop`]=title(n)+' Crop';j[`item.agri_evolution.${n}_seed`]=title(n)+' Seeds';}return JSON.stringify(j,null,2)+'\n';});
edit(java+'client/journal/JournalIndexScreen.java',s=>s.replace('+"/36 | Page "','+"/"+IllustratedBreedingRecipes.ALL.size()+" | Page "'));
for(const f of ['client/greenhouse/BotanicalGreenhouseScreen.java','client/journal/MachineGuidePages.java'])edit(java+f,s=>s.replaceAll('Wheat / Flowers','Food / Flowers'));
edit(java+'client/greenhouse/BotanicalGreenhouseHarvestScreen.java',s=>s.replace('Flowers / Other','Food / Flowers'));
edit('gradle.properties',s=>s.replace('mod_version=0.48.0','mod_version=0.49.0'));
console.log('Added eight resource crops, original assets and breeding routes.');
