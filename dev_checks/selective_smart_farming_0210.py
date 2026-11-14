#!/usr/bin/env python3
"""Source-level regression for v0.21.0 greenhouse seed filters; Minecraft runtime test still required."""
from pathlib import Path
import re, json, hashlib
root=Path(__file__).resolve().parents[1]
j=root/'src/main/java/com/agrievolution'
be=(j/'content/BotanicalGreenhouseBlockEntity.java').read_text()
menu=(j/'content/BotanicalGreenhouseMenu.java').read_text()
ui=(j/'client/greenhouse/BotanicalGreenhouseScreen.java').read_text()
notes=(j/'client/journal/MachineGuidePages.java').read_text()
species=(j/'genetics/CropSpecies.java').read_text()
# Regression checks also apply to all subsequent compatible Agri Evolution versions.
# Since 0.29.5 the existing 42 dashboard fields remain at indices 0-41,
# followed by six additional read-only genetic-monitoring fields (42-47).
import re
version = re.search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', (root/'gradle.properties').read_text())
assert version and tuple(map(int, version.groups())) >= (0, 24, 0)
assert 'private boolean plantFlowerSeeds = true;' in be
assert 'private boolean plantResourceSeeds = true;' in be
assert 'resource ? !plantResourceSeeds : !plantFlowerSeeds' in be
assert 'seedType.species().isResource()' in be
assert 'tag.putBoolean("SmartFarmingFlowerFilter", plantFlowerSeeds)' in be
assert 'tag.putBoolean("SmartFarmingResourceFilter", plantResourceSeeds)' in be
assert 'tag.contains("SmartFarmingFlowerFilter")' in be
assert 'tag.contains("SmartFarmingResourceFilter")' in be
assert 'private void plantFromSeedBank(ServerLevel server)' in be
assert re.search(r'case 31 -> plantFlowerSeeds \? 1 : 0;',be)
assert re.search(r'case 32 -> plantResourceSeeds \? 1 : 0;',be)
assert 'getCount() { return 51; }' in be
assert 'new SimpleContainerData(51)' in menu and 'checkContainerDataCount(data, 51);' in menu
for i in (7,8):
 assert f'button == {i}' in menu
 assert f'menu.containerId, {i}' not in ui  # uses dynamic target for buttons
assert 'flowerPlantingEnabled()' in menu and 'resourcePlantingEnabled()' in menu
assert 'Wheat / Flowers: ' in ui and 'Resource Crops: ' in ui
assert 'SMART FARMING FILTERS' in notes
assert 'public boolean isResource() { return ordinal() >= DIRT.ordinal(); }' in species
# Absolutely no changes to approved seed texture files (across 39 species).
assets=root/'src/main/resources/assets/agri_evolution'
for filename, checksum in json.loads((root/'WORKING_TEXTURE_SHA256.json').read_text()).items():
 assert hashlib.sha256((assets/filename).read_bytes()).hexdigest()==checksum, filename
print('PASS: v0.21.0 smart-farming filters, sync, controls, save defaults, flower/resource classification, approved texture SHA256')
