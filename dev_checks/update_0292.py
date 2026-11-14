#!/usr/bin/env python3
"""Static regression checks for the soil table v0.29.2 patch."""
from pathlib import Path
P=Path(__file__).resolve().parents[1]
J=P/'src/main/java/com/agrievolution'
lab=(J/'content/SoilResearchTableBlockEntity.java').read_text()
menu=(J/'content/SoilResearchTableMenu.java').read_text()
screen=(J/'client/soil/SoilResearchTableScreen.java').read_text()
journal=(J/'client/journal/MachineGuidePages.java').read_text()
assert tuple(map(int, __import__('re').search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', (P/'gradle.properties').read_text()).groups())) >= (0, 29, 2)
assert 'new SimpleContainerData(4)' in menu
assert 'checkContainerDataCount(data, 4)' in menu
assert 'public int lastBatch()' in menu
assert 'lastBatch = studied' in lab
assert 'lastBatch = batch' in lab
assert 'tag.putInt("LastBatch", lastBatch)' in lab
assert 'int batchLimit = tableTier == 1 ? 1 : tableTier == 2 ? 2 : 4;' in lab
assert 'int batchLimit = data.get(0) == 1 ? 1 : data.get(0) == 2 ? 2 : 4;' in lab
assert 'Math.min(source.getCount(), feed.getCount())' in lab
assert 'source.shrink(batch);' in lab and 'feed.shrink(batch);' in lab
assert 'new ItemStack(result, batch)' in lab
assert 'if (!SoilResearch.analyze(tool, specimen)) break;' in lab
assert 'specimen.shrink(1)' in lab
assert 'hoveredSlot != null' in screen
assert 'font.split(Component.literal(tip), 170)' in screen
assert 'imageHeight = 264' in screen
for tier in ['SOIL LAB HOW-TO','FARMLAND TIERS 1-3','SOIL LAB MK II','SOIL LAB MK III','ADVANCED SOIL TIPS']:
 assert tier in journal, tier
for component in ['Nutrient Compost','Copper','Amethyst','Gold Ingot','Echo Shard']:
 assert component in journal, component
assert 'crouch Copper Block' not in journal and 'Amethyst Block to get' not in journal
print('PASS 0.29.2: batch research/refine, conservation, UI tooltip spacing, saved batch totals, new journal guides')
