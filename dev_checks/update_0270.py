#!/usr/bin/env python3
"""Static checks for Agri Evolution v0.28.0. Not a substitute for Gradle or in-game tests."""
from pathlib import Path
import hashlib, json
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/com/agrievolution'
RES=ROOT/'src/main/resources'

def contents(name):
    return (JAVA/name).read_text()

def assert_contains(name,*needles):
    source=contents(name)
    for needle in needles:
        assert needle in source,(name,needle)

def main():
    recipe=json.loads((RES/'data/agri_evolution/recipe/soil_sample.json').read_text())
    assert recipe['type']=='minecraft:crafting_shaped'
    assert recipe['result']['id']=='agri_evolution:soil_sample'
    assert {item['item'] for item in recipe['key'].values()} >= {
        'minecraft:glass_bottle','minecraft:lever','minecraft:iron_ingot','minecraft:copper_ingot','minecraft:redstone'}
    assert len(recipe['pattern'])==3 and all(len(row)==3 for row in recipe['pattern'])
    assert_contains(Path('client/analyzer/SeedAnalyzerScreen.java'),
        'enum Page { PROFILE, COMPARE, BREEDING, RESEARCH }','renderResearch(gui)',
        'HybridLineageStorage.fromSeed(sample)','RareTraitStorage.fromSeed(sample)',
        'JournalSnapshot.from(other)','journalRecord.entry(species).discovered()')
    assert_contains(Path('content/IrrigationBasinBlockEntity.java'),
        'IrrigationUpgradeTier','tag.putInt("IrrigationUpgradeTier", upgradeTier)',
        'wateringRadius()','public static void serverTick','stopWatering();')
    assert_contains(Path('content/IrrigationBasinBlock.java'),
        'IrrigationBasinBlockEntity.serverTick','useWithoutItem')
    assert_contains(Path('content/IrrigationBasinUpgradeHandler.java'),
        'Items.COPPER_BLOCK','Items.AMETHYST_BLOCK','setCancellationResult','held.shrink(1)')
    assert_contains(Path('farming/FarmEnvironment.java'),
        'MAX_IRRIGATION_RANGE = 10','basin.wateringRadius()')
    assert_contains(Path('client/journal/MachineGuidePages.java'),
        'SOIL SAMPLER','SEED RESEARCH','IRRIGATION UPGRADES')
    # Existing confirmed-good textures must not change (this test caught earlier regressions).
    hashes=json.loads((ROOT/'WORKING_TEXTURE_SHA256.json').read_text())
    for local,hexdigest in hashes.items():
        tex=RES/'assets/agri_evolution'/local
        assert tex.exists(),local
        assert hashlib.sha256(tex.read_bytes()).hexdigest()==hexdigest,local
    for name in ('cultivated','nutrient','mythic'):
        for suffix in ('dry','wet','side'):
            f=RES/f'assets/agri_evolution/textures/block/{name}_farmland_{suffix}.png'
            assert f.exists() and f.read_bytes()[:8]==b'\x89PNG\r\n\x1a\n',f
    assert tuple(map(int, __import__('re').search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', (ROOT/'gradle.properties').read_text()).groups())) >= (0, 28, 0)
    print('0.28.0 legacy feature checks PASS: sampler recipe, research dossier, irrigation tiers,')
    print('  persistent upgrade NBT, guidebook, 12 protected sprites and advanced soil textures')
    print('  Note: compile/runtime validation still requires NeoForge 21.1.252 + Gradle.')

if __name__=='__main__': main()
