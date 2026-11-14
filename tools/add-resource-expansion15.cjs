// One-time, additive 0.68.0 content migration. Original art lives in artwork/resource-expansion-v15.
const fs=require('fs'),path=require('path');
const names=["sugar_cane","sweet_berries","seagrass","hanging_roots","torchflower","pitcher_plant","spore_blossom","big_dripleaf","trial_key","ominous_trial_key","netherite_upgrade_smithing_template","disc_fragment_5"];
const java='src/main/java/com/agrievolution/',res='src/main/resources/';
if(fs.readFileSync(java+'genetics/CropSpecies.java','utf8').includes('SUGAR_CANE('))throw new Error('Resource crop migration already applied; use prepare-sprites.ps1 only to rebuild its assets.');
const edit=(p,fn)=>fs.writeFileSync(p,fn(fs.readFileSync(p,'utf8')));
const title=n=>n==='ender_eye'?'Eye of Ender':n.split('_').map(w=>w[0].toUpperCase()+w.slice(1)).join(' '),up=n=>n.toUpperCase();
edit(java+'genetics/CropSpecies.java',s=>s.replace('"enchanted_golden_apple_seed", true);','"enchanted_golden_apple_seed", true),\n'+names.map(n=>`    ${up(n)}("${n}", "${title(n)}", "${n}_crop", "${n}_seed", true)`).join(',\n')+';'));
edit(java+'content/AEItems.java',s=>s.replace('    public static final DeferredItem<Item> BREEDING_JOURNAL',names.map(n=>`    public static final DeferredItem<Item> ${up(n)}_SEED = ITEMS.registerItem("${n}_seed", props -> new SpeciesSeedItem(props, CropSpecies.${up(n)}, AEBlocks.${up(n)}_CROP::get), new Item.Properties());`).join('\n')+'\n\n    public static final DeferredItem<Item> BREEDING_JOURNAL').replace('case NETHERITE -> NETHERITE_SEED.get();','case NETHERITE -> NETHERITE_SEED.get();\n'+names.map(n=>`            case ${up(n)} -> ${up(n)}_SEED.get();`).join('\n')));
edit(java+'content/AEBlocks.java',s=>s.replace('    public static final DeferredBlock<AgriCropBlock> NETHERITE_CROP',names.map(n=>`    public static final DeferredBlock<AgriCropBlock> ${up(n)}_CROP = BLOCKS.register("${n}_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.${up(n)}, AEItems.${up(n)}_SEED::get));`).join('\n')+'\n    public static final DeferredBlock<AgriCropBlock> NETHERITE_CROP'));
edit(java+'content/AEBlockEntities.java',s=>s.replace('AEBlocks.ENCHANTED_GOLDEN_APPLE_CROP.get()).build', 'AEBlocks.ENCHANTED_GOLDEN_APPLE_CROP.get(), '+names.map(n=>`AEBlocks.${up(n)}_CROP.get()`).join(', ')+').build'));
edit(java+'content/AECreativeTabs.java',s=>s.replace('output.accept(AEItems.BREEDING_JOURNAL.get());',names.map(n=>`output.accept(AEItems.${up(n)}_SEED.get());`).join('\n                        ')+'\n                        output.accept(AEItems.BREEDING_JOURNAL.get());'));
for(const file of ['content/AgriCropBlock.java','content/BotanicalGreenhouseBlockEntity.java'])edit(java+file,s=>s.replace('case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produce(species);', 'case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produce(species);\n'+names.map(n=>`            case ${up(n)} -> com.agrievolution.farming.ResourceCropRules.produce(species);`).join('\n')).replace('case NETHERITE -> AEBlocks.NETHERITE_CROP.get();','case NETHERITE -> AEBlocks.NETHERITE_CROP.get();\n'+names.map(n=>`                    case ${up(n)} -> AEBlocks.${up(n)}_CROP.get();`).join('\n')));
for(const [file,v] of [['CompactJournalRenderer.java','plant'],['JournalScreen.java','species']])edit(java+'client/journal/'+file,s=>s.replace(`case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produceName(${v});`,`case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produceName(${v});\n            case ${names.map(up).join(', ')} -> com.agrievolution.farming.ResourceCropRules.produceName(${v});`));
const crosses=[["SUGAR","PAPER","SUGAR_CANE",28],["STRAWBERRY","VINE","SWEET_BERRIES",26],["MOSS","KELP","SEAGRASS",26],["ROOTED_DIRT","VINE","HANGING_ROOTS",26],["AMBER_MARIGOLD","SUNFIRE_LILY","TORCHFLOWER",18],["CACTUS","LILY_PAD","PITCHER_PLANT",18],["MYCELIUM","VIOLET_STARFLOWER","SPORE_BLOSSOM",16],["LILY_PAD","JUNGLE_LOG","BIG_DRIPLEAF",20]];
const labCrosses=[["BREEZE_ROD","COPPER","TRIAL_KEY",5],["TRIAL_KEY","ECHO_SHARD","OMINOUS_TRIAL_KEY",3],["ANCIENT_DEBRIS","PAPER","NETHERITE_UPGRADE_SMITHING_TEMPLATE",3],["ECHO_SHARD","BLACK_DYE","DISC_FRAGMENT_5",5]];
edit(java+'genetics/AdvancedHybridBreeder.java',s=>s.replace('        return null;',labCrosses.map(([a,b,c,p])=>`        if (pair(first, second, CropSpecies.${a}, CropSpecies.${b}))\n            return new HybridRecipe(CropSpecies.${c}, ${p});`).join('\n')+'\n        return null;'));
edit(java+'genetics/SpeciesBreeder.java',s=>s.replace('        return null;',crosses.map(([a,b,c,p])=>`        if (pair(first, second, CropSpecies.${a}, CropSpecies.${b}))\n            return new MutationRecipe(CropSpecies.${c}, ${p});`).join('\n')+'\n        return null;'));
const write=(p,v)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');};
const assets=res+'assets/agri_evolution/';
for(const n of names){
 for(const p of ['blockstates/crimson_clover_crop.json',...Array.from({length:8},(_,i)=>`models/block/crimson_clover_crop_stage${i}.json`),'models/item/crimson_clover_seed.json']){
  fs.writeFileSync(assets+p.replaceAll('crimson_clover',n),fs.readFileSync(assets+p,'utf8').replaceAll('crimson_clover',n));
 }
 const loot=JSON.parse(fs.readFileSync(res+'data/agri_evolution/loot_table/blocks/crimson_clover_crop.json','utf8').replaceAll('crimson_clover',n));
 loot.pools[0].entries[0].name='minecraft:'+n;
 if(["trial_key","ominous_trial_key","netherite_upgrade_smithing_template","disc_fragment_5"].includes(n))loot.pools[0].entries[0].functions=[];
 // Mature player harvest supplies the genetic seed in AgriCropBlock.playerDestroy.
 loot.pools[1].entries[0].conditions=[{condition:'minecraft:inverted',term:loot.pools[1].entries[0].conditions[0]}];
 write(res+`data/agri_evolution/loot_table/blocks/${n}_crop.json`,loot);
 // Textures installed after all original sprite sheets are verified.
}
edit(assets+'lang/en_us.json',s=>{const j=JSON.parse(s);for(const n of names){j[`block.agri_evolution.${n}_crop`]=title(n)+' Crop';j[`item.agri_evolution.${n}_seed`]=title(n)+' Seeds';}return JSON.stringify(j,null,2)+'\n';});
edit(java+'client/journal/JournalIndexScreen.java',s=>s.replace('+"/36 | Page "','+"/"+IllustratedBreedingRecipes.ALL.size()+" | Page "'));
for(const f of ['client/greenhouse/BotanicalGreenhouseScreen.java','client/journal/MachineGuidePages.java'])edit(java+f,s=>s.replaceAll('Wheat / Flowers','Food / Flowers'));
edit(java+'client/greenhouse/BotanicalGreenhouseHarvestScreen.java',s=>s.replace('Flowers / Other','Food / Flowers'));
edit('gradle.properties',s=>s.replace('mod_version=0.68.0','mod_version=0.69.0'));
console.log('Added twelve resource crops, original assets and breeding routes.');
