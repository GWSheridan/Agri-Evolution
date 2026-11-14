#!/usr/bin/env python3
"""0.29 source/resource guard. Not a NeoForge compile or in-game test."""
from pathlib import Path
import json,hashlib
P=Path(__file__).resolve().parents[1]
J=P/'src/main/java/com/agrievolution'
A=P/'src/main/resources/assets/agri_evolution'
D=P/'src/main/resources/data/agri_evolution'
assert tuple(map(int, __import__('re').search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', (P/'gradle.properties').read_text()).groups())) >= (0, 29, 1)
for cls in ['SoilResearchTableBlock','SoilResearchTableBlockEntity','SoilResearchTableMenu', 'SoilResearchTableUpgradeHandler']:
    assert (J/'content'/f'{cls}.java').exists(),cls
assert (J/'client/soil/SoilResearchTableScreen.java').exists()
assert (J/'client/soil/SoilResearchTableClient.java').exists()
assert 'SOIL_RESEARCH_TABLE' in (J/'content/AEBlockEntities.java').read_text()
assert 'SOIL_RESEARCH_TABLE' in (J/'content/AEMenus.java').read_text()
assert 'SOIL_RESEARCH_MASTERY_UPGRADE' in (J/'content/SoilResearchTableBlock.java').read_text()
assert 'Containers.dropContents' in (J/'content/SoilResearchTableBlock.java').read_text()
for milestone in ('count >= 8','count >= 24','count >= 48','count >= 72','count >= 96'):
    assert milestone in (J/'farming/SoilResearch.java').read_text(),milestone
assert 'MAX = 96' in (J/'farming/SoilResearch.java').read_text()
assert '1 << soilTier' in (J/'content/AgriCropBlock.java').read_text()
assert 'int steps = 1 + tier' in (J/'content/BotanicalGreenhouseBlockEntity.java').read_text()
assert 'int extra = Math.max(1, tier - 1);' in (J/'content/AdvancedFarmlandBlock.java').read_text()
lang=json.loads((A/'lang/en_us.json').read_text())
for kind in ('arcane','celestial'):
    name=kind+'_farmland'
    assert f'"{name}"' in (J/'content/AEBlocks.java').read_text()
    assert f'"{name}"' in (J/'content/AEItems.java').read_text()
    assert lang['block.agri_evolution.'+name]
    for suf in ('dry','wet'):
        model=json.loads((A/'models/block'/f'{name}_{suf}.json').read_text())
        assert model['elements'][0]['to']==[16,15,16]
        assert model['textures']['top'].startswith('minecraft:block/farmland')
        assert (A/'textures/block'/f'{name}_{suf}.png').read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
    assert (A/'models/item'/f'{name}.json').exists()
    assert (D/'loot_table/blocks'/f'{name}.json').exists()
    states=json.loads((A/'blockstates'/f'{name}.json').read_text())['variants']
    assert len(states)==8
for entry in ('facing=north,tier=1','facing=south,tier=2','facing=west,tier=3'):
    assert entry in json.loads((A/'blockstates/soil_research_table.json').read_text())['variants']
for name in ('soil_research_table_mk2','soil_research_table_mk3'):
    assert (A/'models/block'/f'{name}.json').exists()
    for face in ('front','top'):
        assert (A/'textures/block'/f'{name}_{face}.png').read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
for path,sha in json.loads((P/'WORKING_TEXTURE_SHA256.json').read_text()).items():
    assert hashlib.sha256((A/path).read_bytes()).hexdigest()==sha,path
print('0.29 STATIC PASS: research inventory, GUI wiring, table upgrades, five farmland tiers, locked historical textures')
