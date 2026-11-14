"""Regression guard for the Greenhouse's seed-planting and harvest switches.
Run from any directory with: python dev_checks/verify_greenhouse_switches.py
This checks the source without Minecraft/NeoForge dependencies.
"""
from collections import Counter
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
java = root / 'src/main/java/com/agrievolution'
greenhouse = (java / 'content/BotanicalGreenhouseBlockEntity.java').read_text(encoding='utf-8')
species = (java / 'genetics/CropSpecies.java').read_text(encoding='utf-8')
names = re.findall(r'^\s*([A-Z_]+)\("[a-z_]+",', species, re.M)
assert len(names) == 39 and len(set(names)) == 39, 'Expected 39 unique species'


def switch_body(after: str, selector: str) -> str:
    begin = greenhouse.index(after)
    start = greenhouse.index('switch (' + selector + ') {', begin)
    start = greenhouse.index('{', start)
    depth = 1
    for i in range(start + 1, len(greenhouse)):
        if greenhouse[i] == '{':
            depth += 1
        elif greenhouse[i] == '}':
            depth -= 1
            if depth == 0:
                return greenhouse[start + 1:i]
    raise AssertionError('Unclosed switch for ' + selector)


def verify_cases(body: str, description: str) -> None:
    case_names = re.findall(r'\bcase\s+([A-Z_]+)\s*->', body)
    counts = Counter(case_names)
    assert set(counts) == set(names), (description, 'Missing:', set(names)-set(counts), 'Extra:',set(counts)-set(names))
    assert all(count == 1 for count in counts.values()), (description, 'Duplicate species:',counts)

plant = switch_body('private void plantFromSeedBank(', 'seedType.species()')
harvest = switch_body('private static net.minecraft.world.item.Item harvestProduce(', 'species')
verify_cases(plant, 'seed-to-crop mapping')
verify_cases(harvest, 'crop-to-resource mapping')
assert 'ResourceCropRules.produce' not in plant, 'Harvest logic accidentally added to planting switch'
assert 'AEBlocks.' not in harvest, 'Planting logic accidentally added to harvest switch'
for name in ['GRAVEL', 'SAND', 'CLAY', 'FLINT', 'GLOWSTONE', 'OBSIDIAN']:
    assert re.search(r'case\s+'+name+r'\s*->\s*AEBlocks\.'+name+r'_CROP\.get\(\)', plant), name
    assert re.search(r'case\s+'+name+r'\s*->\s*com\.agrievolution\.farming\.ResourceCropRules\.produce\(species\)', harvest), name
print('PASS: all 39 species mapped exactly once in planting and harvesting switches')
