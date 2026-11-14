#!/usr/bin/env python3
"""Offline non-runtime guards for v0.24, together with legacy texture digests."""
from pathlib import Path
import hashlib, json
root=Path(__file__).resolve().parents[1]
java=root/'src/main/java/com/agrievolution'
be=(java/'content/BotanicalGreenhouseBlockEntity.java').read_text()
menu=(java/'content/BotanicalGreenhouseHarvestMenu.java').read_text()
screen=(java/'client/greenhouse/BotanicalGreenhouseHarvestScreen.java').read_text()
controller=(java/'client/greenhouse/BotanicalGreenhouseScreen.java').read_text()
registry=(java/'content/AEMenus.java').read_text()
client=(java/'client/greenhouse/BotanicalGreenhouseClient.java').read_text()
# Regression checks also apply to all subsequent compatible Agri Evolution versions.
import re
version = re.search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', (root/'gradle.properties').read_text())
assert version and tuple(map(int, version.groups())) >= (0, 24, 0)
assert 'HarvestExportEnabled' in be and 'HarvestExportSide' in be
assert 'exportToNeighbor(server)' in be and 'compactStorage()' in be
assert 'server.hasChunkAt(destination)' in be and 'getCapability(Capabilities.ItemHandler.BLOCK' in be
assert 'handler.insertItem(slot, remaining, true)' in be
assert 'handler.insertItem(slot, remaining.copyWithCount(accepted), false)' in be
assert 'inSlot.shrink(moved)' in be
assert 'ItemStack.isSameItemSameComponents(target, source)' in be
assert 'getCount() { return 15; }' in be and 'SimpleContainerData(15)' in menu
assert 'toggleExportCategory' in menu and 'cycleExportPriority' in menu
assert 'openHarvestManagement(server)' in (java/'content/BotanicalGreenhouseMenu.java').read_text()
assert 'HARVEST SETTINGS' in controller and 'OPEN STORAGE' in controller
assert 'BOTANICAL_GREENHOUSE_HARVEST' in registry and 'BOTANICAL_GREENHOUSE_HARVEST' in client
assert 'handleInventoryButtonClick' in screen
assets=root/'src/main/resources/assets/agri_evolution'
for file, expected in json.loads((root/'WORKING_TEXTURE_SHA256.json').read_text()).items():
    assert hashlib.sha256((assets/file).read_bytes()).hexdigest() == expected, file
print('PASS: v0.24 output policy, menu, saves, capability transfer, no changes to approved sprites')
