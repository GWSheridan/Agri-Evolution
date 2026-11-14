#!/usr/bin/env python3
"""Static 0.26 progression and asset checks; NeoForge compilation/gameplay still required."""
from pathlib import Path
import hashlib,json,re
root=Path(__file__).resolve().parents[1]
a=root/'src/main/resources/assets/agri_evolution'
j=root/'src/main/java/com/agrievolution'
d=root/'src/main/resources/data/agri_evolution'
blocks=(j/'content/AEBlocks.java').read_text()
items=(j/'content/AEItems.java').read_text()
soil=(j/'content/AdvancedFarmlandBlock.java').read_text()
research=(j/'farming/SoilResearch.java').read_text()
crop=(j/'content/AgriCropBlock.java').read_text()
seeds=(j/'content/SpeciesSeedItem.java').read_text()
greenhouse=(j/'content/BotanicalGreenhouseBlockEntity.java').read_text()
sampler=(j/'content/SoilSampleItem.java').read_text()
lang=json.loads((a/'lang/en_us.json').read_text())
assert ('MAX = 48' in research or 'MAX = 96' in research) and all(s in research for s in ('count >= 48','count >= 24','count >= 8'))
assert 'AgriSoilResearch' in research and 'plot.asLong()' in research
assert 'SoilResearch.gather(sampler, level, soil, area)' in sampler
assert 'SoilSpecimenItem.from(level, soil)' in sampler and 'SoilResearch.analyze(' in ((j/'content/SoilResearchTableBlock.java').read_text() + (j/'content/SoilResearchTableBlockEntity.java').read_text())
assert 'protected boolean mayPlaceOn(' in crop
assert 'chance + soilTier * 20' in crop and '3 + soilTier * 2' in crop
assert 'SoilResearch.isSoil' in seeds and 'SoilResearch.isSoil' in greenhouse
assert 'instanceof FarmBlock' in greenhouse
assert 'level.setBlock(pos, state.setValue(MOISTURE, next)' in soil
assert 'level.setBlock(pos, upgraded, 3)' not in sampler
for name in ('cultivated_farmland','nutrient_farmland','mythic_farmland'):
    assert ('"'+name+'"') in blocks and ('"'+name+'"') in items
    assert lang['block.agri_evolution.'+name]
    assert (a/'models/item'/f'{name}.json').is_file()
    assert (d/'loot_table/blocks'/f'{name}.json').is_file()
    state=json.loads((a/'blockstates'/f'{name}.json').read_text())
    assert len(state['variants'])==8
    for v in ('dry','wet'):
        model=json.loads((a/'models/block'/f'{name}_{v}.json').read_text())
        assert model['elements'][0]['to']==[16,15,16]
        path=a/'textures/block'/f'{name}_{v}.png'
        assert path.is_file() and path.read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    assert (a/'textures/block'/f'{name}_side.png').is_file()
for path,expected in json.loads((root/'WORKING_TEXTURE_SHA256.json').read_text()).items():
    assert hashlib.sha256((a/path).read_bytes()).hexdigest()==expected, path
print('0.26 soil research, all 3 block families and 12 protected textures: static check OK')
