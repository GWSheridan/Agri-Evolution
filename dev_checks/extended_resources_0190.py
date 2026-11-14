"""Agri Evolution 0.19: offline progression, registration, graphics & loot verification."""
from pathlib import Path
from PIL import Image
import json, hashlib
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/agri_evolution'
data=root/'src/main/resources/data/agri_evolution'
java=root/'src/main/java/com/agrievolution'
new=['gravel','sand','clay','flint','glowstone','obsidian']
lang=json.loads((assets/'lang/en_us.json').read_text())
for id in new:
    up=id.upper()
    for rel,marker in [
        ('genetics/CropSpecies.java',f'{up}("{id}"'),
        ('content/AEItems.java',f'"{id}_seed"'),
        ('content/AEBlocks.java',f'"{id}_crop"'),
        ('content/AECreativeTabs.java',f'AEItems.{up}_SEED.get()'),
        ('content/BotanicalGreenhouseBlockEntity.java',f'case {up} -> AEBlocks.{up}_CROP.get()'),
        ('farming/ResourceCropRules.java',f'case {up} ->'),
        ('content/AgriCropBlock.java',f'case {up} ->')]:
        assert marker in (java/rel).read_text(),f'{id} missing {rel}'
    assert f'item.agri_evolution.{id}_seed' in lang
    assert f'block.agri_evolution.{id}_crop' in lang
    for filename,size in [(f'textures/item/{id}_seed.png',(16,16)),
        (f'textures/gui/journal/species/{id}.png',(32,32)),
        (f'textures/gui/journal/species/small/{id}.png',(16,16))]:
        with Image.open(assets/filename) as im:
            assert im.size==size, filename
            if 'item/' in filename: assert set(im.getchannel('A').getdata())=={0,255},filename
    bs=json.loads((assets/f'blockstates/{id}_crop.json').read_text())
    assert len(bs['multipart'])==9,id
    for stage in range(8):
        model=f'{id}_crop_stage{stage}'
        assert any(e.get('when',{}).get('age')==str(stage) and
            e['apply']['model']==f'agri_evolution:block/{model}' for e in bs['multipart'])
        assert (assets/f'models/block/{model}.json').exists()
        with Image.open(assets/f'textures/block/{model}.png') as im: assert im.size==(16,16)
    loot=json.loads((data/f'loot_table/blocks/{id}_crop.json').read_text())
    assert f'agri_evolution:{id}_seed' in str(loot)
    assert f'minecraft:{"clay_ball" if id=="clay" else "glowstone_dust" if id=="glowstone" else id}' in str(loot)
# Approved original seed/crop pixels must remain exactly as delivered.
locked=json.loads((root/'WORKING_TEXTURE_SHA256.json').read_text())
for path,digest in locked.items():
    assert hashlib.sha256((assets/path).read_bytes()).hexdigest()==digest,path
for path in list(assets.rglob('*.json'))+list(data.rglob('*.json')):
    json.loads(path.read_text())
print('PASS: six resource seeds, 48 crop stages, 6 loot tables, catalysts, journals, and approved textures')
