const fs=require('fs');let s=fs.readFileSync('tools/add-resource-expansion5.cjs','utf8');
s=s.replaceAll('resource-expansion-v5','resource-expansion-v6').replace('0.53.0 content','0.54.0 content')
.replace(/const names=\[.*?\];/,'const names='+"[\"red_sand\",\"sandstone\",\"cactus\",\"vine\",\"packed_mud\",\"brick\",\"ice\",\"packed_ice\",\"blue_ice\",\"magma_block\",\"nether_brick\",\"ender_eye\"]"+';')
.replace("includes('GRANITE(')","includes('RED_SAND(')")
.replaceAll('"sponge_seed", true)','"sculk_seed", true)')
.replaceAll('AEBlocks.SPONGE_CROP.get()', 'AEBlocks.SCULK_CROP.get()')
.replace(/const crosses=\[.*?\];/,'const crosses='+"[[\"SAND\",\"GRANITE\",\"RED_SAND\",25],[\"SAND\",\"STONE\",\"SANDSTONE\",28],[\"SAND\",\"WILD\",\"CACTUS\",25],[\"MOSS\",\"STRING\",\"VINE\",25],[\"MUD\",\"WILD\",\"PACKED_MUD\",22],[\"CLAY\",\"TERRACOTTA\",\"BRICK\",22],[\"SNOWBALL\",\"CALCITE\",\"ICE\",20],[\"ICE\",\"STONE\",\"PACKED_ICE\",18]]"+';')
.replace(/const labCrosses=\[.*?\];/,'const labCrosses='+"[[\"PACKED_ICE\",\"DIAMOND\",\"BLUE_ICE\",10],[\"MAGMA_CREAM\",\"OBSIDIAN\",\"MAGMA_BLOCK\",12],[\"NETHERRACK\",\"COAL\",\"NETHER_BRICK\",12],[\"ENDER_PEARL\",\"BLAZE_ROD\",\"ENDER_EYE\",8]]"+';')
.replace("(n==='dripstone'?'pointed_dripstone':n)",'n')
.replace("['sea_lantern','shroomlight','end_rod','sculk']","['blue_ice','magma_block','nether_brick','ender_eye']")
.replace("s.replace('mod_version=0.52.0','mod_version=0.53.0')","s.replace('mod_version=0.53.1','mod_version=0.54.0')")
.replace("const title=n=>n.split('_')","const title=n=>n==='ender_eye'?'Eye of Ender':n.split('_')");
fs.writeFileSync('tools/add-resource-expansion6.cjs',s);

