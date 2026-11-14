#!/usr/bin/env python3
"""Offline 0.20.0 regression: metadata, 11 seed/crop asset chains, loot, code and fingerprints.

Does not substitute for in-game planting, craft/recipe or NeoForge Gradle build.
"""
from pathlib import Path
from PIL import Image
import json, hashlib, re

root=Path(__file__).resolve().parents[1]
a=root/'src/main/resources/assets/agri_evolution'
d=root/'src/main/resources/data/agri_evolution/loot_table/blocks'
j=root/'src/main/java/com/agrievolution'
old=["wild","crimson_clover","azure_flax","violet_starflower","amber_marigold","silver_thistle","sunfire_lily","moon_orchid","eclipse_lotus","twilight_sage","dirt","wood","stone","coal","copper","iron","redstone","gold","lapis","diamond","emerald","quartz","gravel","sand","clay","flint","glowstone","obsidian"]
new={
    'netherrack':('QUARTZ','OBSIDIAN',14,'netherrack'),
    'soul_sand':('NETHERRACK','SAND',16,'soul_sand'),
    'magma_cream':('NETHERRACK','GLOWSTONE',12,'magma_cream'),
    'nether_wart':('SOUL_SAND','CRIMSON_CLOVER',14,'nether_wart'),
    'blaze_rod':('MAGMA_CREAM','NETHER_WART',10,'blaze_rod'),
    'end_stone':('OBSIDIAN','EMERALD',12,'end_stone'),
    'chorus_fruit':('END_STONE','AZURE_FLAX',12,'chorus_fruit'),
    'ender_pearl':('END_STONE','CHORUS_FRUIT',8,'ender_pearl'),
    'shulker_shell':('ENDER_PEARL','CHORUS_FRUIT',6,'shulker_shell'),
    'ancient_debris':('BLAZE_ROD','OBSIDIAN',7,'ancient_debris'),
    'netherite':('ANCIENT_DEBRIS','ENDER_PEARL',4,'netherite_scrap'),
}
version_text = (root/'gradle.properties').read_text()
version_match = re.search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', version_text)
assert version_match and tuple(map(int, version_match.groups())) >= (0, 20, 0), 'Expected 0.20.0 or later' 
cs=(j/'genetics/CropSpecies.java').read_text()
ids=re.findall(r'(?m)^\s*([A-Z_]+)\("([a-z_]+)",',cs)
assert len(ids)==39,ids
assert [id for _,id in ids][:28]==old, 'Existing species order changed (save compatibility regression)'
assert [id for _,id in ids][28:]==list(new), 'Expansion species order mismatch'
lang=json.loads((a/'lang/en_us.json').read_text())
advanced=(j/'genetics/AdvancedHybridBreeder.java').read_text()
be=(j/'content/AEBlockEntities.java').read_text()
items=(j/'content/AEItems.java').read_text()
blocks=(j/'content/AEBlocks.java').read_text()
tabs=(j/'content/AECreativeTabs.java').read_text()
rule=(j/'farming/ResourceCropRules.java').read_text()
greenhouse=(j/'content/BotanicalGreenhouseBlockEntity.java').read_text()
for name,(left,right,chance,drop) in new.items():
 up=name.upper(); seed=f'{name}_seed'; crop=f'{name}_crop'
 for code,marker in [
    (items, f'"{seed}"'),(blocks,f'"{crop}"'),(tabs,f'AEItems.{up}_SEED.get()'),
    (be,f'AEBlocks.{up}_CROP.get()'),(rule,f'case {up} ->'),
    (greenhouse,f'case {up} -> AEBlocks.{up}_CROP.get()'),
    (advanced,f'new HybridRecipe(CropSpecies.{up}, {chance})')]:
     assert marker in code,(name,marker)
 assert f'item.agri_evolution.{seed}' in lang
 assert f'block.agri_evolution.{crop}' in lang
 assert re.search(rf'pair\(first, second, CropSpecies\.{left}, CropSpecies\.{right}\)\)\s*return new HybridRecipe\(CropSpecies\.{up}, {chance}\)',advanced),name
 for path,size in [(f'textures/item/{seed}.png',(16,16)),(f'textures/gui/journal/species/{name}.png',(32,32)),(f'textures/gui/journal/species/small/{name}.png',(16,16))]:
    with Image.open(a/path) as im:
     assert im.size==size,(path,im.size)
     assert set(im.getchannel('A').getdata())=={0,255},path
 assert json.loads((a/f'models/item/{seed}.json').read_text())['textures']['layer0']==f'agri_evolution:item/{seed}'
 states=json.loads((a/f'blockstates/{crop}.json').read_text())
 assert len(states['multipart'])==9
 for i in range(8):
    model=f'{crop}_stage{i}'
    assert any(x.get('when',{}).get('age')==str(i) and x['apply']['model']==f'agri_evolution:block/{model}' for x in states['multipart'])
    assert (a/f'models/block/{model}.json').exists()
    with Image.open(a/f'textures/block/{model}.png') as im: assert im.size==(16,16)
 loot=json.loads((d/f'{crop}.json').read_text())
 entries=[entry for pool in loot['pools'] for entry in pool['entries']]
 assert {e['name'] for e in entries}=={f'agri_evolution:{seed}',f'minecraft:{drop}'},name
 assert all(e['conditions'][0]['block']==f'agri_evolution:{crop}' and e['conditions'][0]['properties']['age']=='7' for e in entries)
 if name in {'ancient_debris','netherite','shulker_shell'}:
    entry=next(x for x in entries if x['name']==f'minecraft:{drop}')
    assert entry['functions'][0]['count']['max']==1.0
# Preserve every formerly approved sprite exactly.
for path,sha in json.loads((root/'WORKING_TEXTURE_SHA256.json').read_text()).items():
 assert hashlib.sha256((a/path).read_bytes()).hexdigest()==sha,path
# Verify all JSON, including loot, client art, tags etc.
for path in (root/'src/main/resources').rglob('*.json'): json.loads(path.read_text())
print('PASS: 39 stable species, 11 Tier-3 recipes, 88 crop stages, 11 loot tables, catalysts, journals, crisp seeds, and approved legacy textures')
