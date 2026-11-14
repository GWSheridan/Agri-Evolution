#!/usr/bin/env python3
"""0.28.0 invariant checks. Java compilation and runtime gameplay must still be tested."""
from pathlib import Path
import json,hashlib
ROOT=Path(__file__).resolve().parents[1];S=ROOT/'src/main';J=S/'java/com/agrievolution';A=S/'resources/assets/agri_evolution';D=S/'resources/data/agri_evolution'

def has(name,*tokens):
    txt=(J/name).read_text()
    for tok in tokens: assert tok in txt,(name,tok)
    return txt

assert tuple(map(int, __import__('re').search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', (ROOT/'gradle.properties').read_text()).groups())) >= (0, 28, 0)
has('AgriEvolution.java','IrrigationBasinUpgradeHandler::onRightClick')
has('content/IrrigationBasinUpgradeHandler.java','PlayerInteractEvent.RightClickBlock','event.setCanceled(true)','event.setCancellationResult','basin.upgradeTo(current + 1)','held.shrink(1)')
has('content/IrrigationBasinBlockEntity.java','tag.putInt("IrrigationUpgradeTier", upgradeTier)','wateringRadius()', 'stopWatering()')
has('content/SoilSampleItem.java','SoilSpecimenItem.from(level, soil)','SoilResearch.collect(sampler, level, soil)', 'SoilSurveyData.survey(level, soil, area)')
assert 'upgrade(level, player' not in (J/'content/SoilSampleItem.java').read_text()
has('content/SoilSpecimenItem.java','"Origin"','DataComponents.CUSTOM_DATA','SoilSpecimenItem(Properties')
has('farming/SoilResearch.java','AgriAnalyzedSoil','AgriCollectedSoil','count >= 48','count >= 24','count >= 8','addUnique')
has('content/SoilResearchTableBlockEntity.java','SoilResearch.analyze','SoilResearch.researchedTier','Items.DIRT','AEItems.CULTIVATED_FARMLAND','AEItems.NUTRIENT_FARMLAND','AEItems.MYTHIC_FARMLAND','NUTRIENT_COMPOST','Items.COPPER_INGOT','Items.AMETHYST_SHARD')
has('content/AgriCropBlock.java','1 << soilTier','chance + soilTier * 20','effectiveMultiplier')
has('content/BotanicalGreenhouseBlockEntity.java','int steps = 1 + tier')
has('content/AEBlocks.java','"soil_research_table"');has('content/AEItems.java','"soil_specimen"','"soil_research_table"')
assert json.loads((D/'recipe/soil_research_table.json').read_text())['result']['id']=='agri_evolution:soil_research_table'
for name in ('soil_research_table','soil_specimen'):
    assert (A/'models/item'/f'{name}.json').exists(),name
    assert ('item.agri_evolution.'+name) in json.loads((A/'lang/en_us.json').read_text())
assert (D/'loot_table/blocks/soil_research_table.json').exists()
assert (A/'textures/block/soil_research_table_top.png').exists()
for local,expected in json.loads((ROOT/'WORKING_TEXTURE_SHA256.json').read_text()).items():
    assert hashlib.sha256((A/local).read_bytes()).hexdigest()==expected,local
for name in ('cultivated','nutrient','mythic'):
    for suffix in ('dry','wet','side'):
        assert (A/'textures/block'/f'{name}_farmland_{suffix}.png').exists()
assert 'enum Page { PROFILE, COMPARE, BREEDING, RESEARCH }' in (J/'client/analyzer/SeedAnalyzerScreen.java').read_text()
print('0.28.0 PASS: early basin handler, real soil specimens, portable research, table recipes/refinement,')
print(' tier scaling, greenhouse assist, preserved Seed Analyzer and all 12 locked textures')
