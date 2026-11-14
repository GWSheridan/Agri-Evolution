"""Lightweight source invariants for greenhouse DNA monitor v0.29.5.

Does not replace a Java/NeoForge build and in-game regression test.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root / 'src/main/java/com/agrievolution'
block = (source / 'content/BotanicalGreenhouseBlockEntity.java').read_text()
menu = (source / 'content/BotanicalGreenhouseMenu.java').read_text()
screen = (source / 'client/greenhouse/BotanicalGreenhouseScreen.java').read_text()
properties = (root / 'gradle.properties').read_text()
import re
version = re.search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', properties)
assert version and tuple(map(int, version.groups())) >= (0, 29, 5)
assert 'getCount() { return 51; }' in block
assert 'new SimpleContainerData(51)' in menu
assert 'checkContainerDataCount(data, 51)' in menu
for i in range(42,48):
    assert f'case {i} ->' in block
for key in ('MonitoringPlantedSpecies', 'MonitoringPlantedSlot',
            'MonitoringPlantedGenome', 'MonitoringPlantedRareTrait'):
    assert block.count('"' + key + '"') >= 2, key
assert 'monitorPlantedGenome = genes;' in block
assert 'monitorPlantedRareTrait = RareTraitStorage.fromSeed(seed);' in block
assert block.index('monitorPlantedGenome = genes;') < block.index('seed.shrink(1);')
assert 'monitorPlantedGenome = genes;' not in block[block.index('private void recordPlantCycle('):block.index('private int chooseSeedBankSlot(')]
for field in ('Last auto-planted:', 'Genetics: Growth ', 'Rare trait: ',
              'menu.lastPlantedGrowth()', 'menu.lastPlantedYield()',
              'menu.lastPlantedResilience()', 'menu.lastPlantedRareTrait()'):
    assert field in screen, field
assert 'monitorPlantedSpecies = Math.clamp(tag.getInt("MonitoringPlantedSpecies")' in block
assert 'tag.contains("MonitoringPlantedGenome", 10)' in block
print('v0.29.5 greenhouse DNA monitor static checks passed')
