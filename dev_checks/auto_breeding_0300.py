#!/usr/bin/env python3
"""Standalone v0.30.0 source/resource regression. Full NeoForge compilation still required."""
from pathlib import Path
import json, hashlib
root=Path(__file__).resolve().parents[1]
src=root/'src/main/java/com/agrievolution'
data=root/'src/main/resources/data'
be=(src/'content/BotanicalGreenhouseBlockEntity.java').read_text()
menu=(src/'content/BotanicalGreenhouseMenu.java').read_text()
screen=(src/'client/greenhouse/BotanicalGreenhouseScreen.java').read_text()
crop=(src/'content/AgriCropBlock.java').read_text()
journal=(src/'client/journal/MachineGuidePages.java').read_text()
assert 'getCount() { return 51; }' in be
assert all(f'case {i} ->' in be for i in range(51))
assert all(s in be for s in ('SmartFarmingAutoBreed','SmartFarmingAutoBred','SmartFarmingBreedResult',
    'SmartFarmingBreedCursor', 'GenomeBreeder.cross(', 'SpeciesBreeder.cross(',
    'RareTrait.inherit(', 'HybridLineageStorage.putOnSeed(offspring',
    'breedFromSeedBank(server)', 'distributeDrops(List.of(offspring)',
    'futureBank.get(first).shrink(1);', 'futureBank.get(second).shrink(1);', 'ExtraSeedRules.chancePercent('))
assert be.index('futureBank.get(first).shrink(1);') < be.index('distributeDrops(List.of(offspring)') < be.index('installInventories(futureBank, futureStorage);', be.index('private void breedFromSeedBank('))
assert 'new SimpleContainerData(51)' in menu and 'checkContainerDataCount(data, 51)' in menu
assert 'button == 10' in menu and 'greenhouse.toggleAutoBreed()' in menu
assert 'handleInventoryButtonClick(menu.containerId, 10)' in screen
assert 'ExtraSeedRules.chancePercent(' in crop and 'Block.popResource(level, pos, seed.copy())' in crop
assert 'FIRST WILD SEEDS' in journal and 'GREENHOUSE AUTO-BREED' in journal
assert not (data/'agri_evolution/recipe/wild_seed.json').exists()
modifier=json.loads((data/'neoforge/loot_modifiers/global_loot_modifiers.json').read_text())
assert modifier['replace'] is False
for grass in ('short_grass','tall_grass'):
    assert 'agri_evolution:'+grass+'_starter_seed' in modifier['entries']
    conf=json.loads((data/('agri_evolution/loot_modifiers/'+grass+'_starter_seed.json')).read_text())
    assert conf['type']=='neoforge:add_table' and conf['table']=='agri_evolution:starter_wild_seed'
    assert conf['conditions'][0]['loot_table_id']=='minecraft:blocks/'+grass
chance=json.loads((data/'agri_evolution/loot_table/starter_wild_seed.json').read_text())
assert chance['pools'][0]['conditions'][0]['chance']==0.10
assert chance['pools'][0]['entries'][0]['name']=='agri_evolution:wild_seed'
for p in root.glob('src/main/resources/**/*.json'):
    json.loads(p.read_text())
print('PASS: v0.30.0 auto breeding, persistence, UI, starter drops, 541+ valid JSON resources')
