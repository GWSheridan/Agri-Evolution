"""Offline source/asset checks for v0.18.0 resource crop progression."""
from pathlib import Path
from PIL import Image
import json,re
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/agri_evolution'
data=root/'src/main/resources/data/agri_evolution'
src=root/'src/main/java/com/agrievolution'
resources={'dirt','wood','stone','coal','copper','iron','redstone','gold','lapis','diamond','emerald','quartz'}
enum=(src/'genetics/CropSpecies.java').read_text()
item=(src/'content/AEItems.java').read_text()
block=(src/'content/AEBlocks.java').read_text()
entities=(src/'content/AEBlockEntities.java').read_text()
creative=(src/'content/AECreativeTabs.java').read_text()
greenhouse=(src/'content/BotanicalGreenhouseBlockEntity.java').read_text()
manual=(src/'content/AgriCropBlock.java').read_text()
lab=(src/'genetics/AdvancedHybridBreeder.java').read_text()
field=(src/'genetics/SpeciesBreeder.java').read_text()
lang=json.loads((assets/'lang/en_us.json').read_text())
count=0
for species in sorted(resources):
    macro=species.upper()
    assert f'{macro}("{species}",' in enum
    assert f'AEBlocks.{macro}_CROP.get()' in entities
    assert f'{macro}_SEED' in item and f'{macro}_CROP' in block
    assert f'AEItems.{macro}_SEED.get()' in creative
    assert f'case {macro} -> AEBlocks.{macro}_CROP.get();' in greenhouse
    assert f'case {macro} ->' in manual
    assert f'case {macro} ->' in greenhouse
    assert f'item.agri_evolution.{species}_seed' in lang
    b=json.loads((assets/f'blockstates/{species}_crop.json').read_text())
    assert len(b['multipart'])==9
    assert b['multipart'][-1]['when']=={'supported':'true'}
    for stage in range(8):
        path=assets/f'textures/block/{species}_crop_stage{stage}.png'
        with Image.open(path) as image:
            assert image.size==(16,16)
            assert image.convert('RGBA').getchannel('A').getbbox()
        model=json.loads((assets/f'models/block/{species}_crop_stage{stage}.json').read_text())
        assert model['textures']['crop']==f'agri_evolution:block/{species}_crop_stage{stage}'
        count+=1
    for tex,size in [(f'textures/item/{species}_seed.png',(16,16)),(f'textures/gui/journal/species/{species}.png',(32,32)),(f'textures/gui/journal/species/small/{species}.png',(16,16))]:
        with Image.open(assets/tex) as im: assert im.size==size
    for suffix in ['models/item/'+species+'_seed.json','loot_table/blocks/'+species+'_crop.json']:
        root2=data if suffix.startswith('loot_table') else assets
        assert (root2/suffix).exists()
    loot=json.loads((data/f'loot_table/blocks/{species}_crop.json').read_text())
    assert f'agri_evolution:{species}_seed' in str(loot)
assert (src/'farming/ResourceCropRules.java').read_text().count('case QUARTZ ->')==5
assert 'DIRT' in field and 'DIAMOND' not in field
assert 'DIAMOND' in lab and 'EMERALD' in lab and 'QUARTZ' in lab
for name in ['dirt','wood']:
    recipe=json.loads((data/f'recipe/{name}_seed.json').read_text())
    assert recipe['result']['id']==f'agri_evolution:{name}_seed'
assert len(list(data.glob('recipe/*_seed.json')))==4, 'Only early flower + resource starter recipes should be available; Wild Seeds now drop from grass'
for path in list(assets.rglob('*.json'))+list(data.rglob('*.json')):
    json.loads(path.read_text())
print(f'PASS: {len(resources)} resources, {count} crop stages, registry mappings, tooltips, recipes, and JSON files')
