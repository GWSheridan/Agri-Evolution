#!/usr/bin/env python3
"""Static source/resource regressions for the 0.25.0 Soil Sampler (not a NeoForge runtime test)."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/com/agrievolution'
RES = ROOT / 'src/main/resources/assets/agri_evolution'

item = (JAVA / 'content/SoilSampleItem.java').read_text()
survey = (JAVA / 'farming/SoilSurveyData.java').read_text()
screen = (JAVA / 'client/soil/SoilSamplerScreen.java').read_text()
registry = (JAVA / 'content/AEItems.java').read_text()
lang = json.loads((RES / 'lang/en_us.json').read_text())

assert 'registerItem("soil_sample", SoilSampleItem::new' in registry, 'Existing item ID changed'
assert lang['item.agri_evolution.soil_sample'] == 'Soil Sampler'
assert (RES / 'textures/item/soil_sample.png').is_file()
assert (RES / 'models/item/soil_sample.json').is_file()
assert 'SoilSurveyData.save(sampler, SoilSurveyData.survey(level, soil, area))' in item
assert 'player.isShiftKeyDown()' in item
assert 'InteractionResultHolder<ItemStack> use(' in item
assert 'context.getItemInHand().shrink' not in item
assert 'SoilSpecimenItem.from(level, soil)' in item and 'SoilResearch.collect(sampler, level, soil)' in item
assert 'upgrade(level, player' not in item
assert 'AgriSoilSurvey' in survey
assert 'FarmEnvironment.irrigated(level, soil)' in survey
assert 'ResourceCropRules.growthMultiplier(' in survey
assert 'FarmEnvironment.growthChance(' in survey
assert 'instanceof AECropBlockEntity' in survey
assert 'out.putInt("Nutrition"' in survey
assert 'out.putInt("Nutrition", 40)' not in survey
assert 'Math.min(25, tiles.size())' in screen
assert 'case AREA -> area(gui)' in screen
assert 'case ADVICE -> advice(gui)' in screen
assert 'JournalFonts.body(text)' in screen
assert 'Minecraft.getInstance().setScreen' in (JAVA/'client/SoilSamplerClientScreens.java').read_text()
print('Agri Evolution 0.25.0 Soil Sampler static checks passed (not a runtime/compile test)')
