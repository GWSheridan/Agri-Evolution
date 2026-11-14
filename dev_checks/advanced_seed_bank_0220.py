#!/usr/bin/env python3
"""Source and approved sprite regression checks; game build/test still required."""
from pathlib import Path
import json, hashlib
root = Path(__file__).resolve().parents[1]
java = root/'src/main/java/com/agrievolution'
be = (java/'content/BotanicalGreenhouseBlockEntity.java').read_text()
menu = (java/'content/BotanicalGreenhouseSeedBankMenu.java').read_text()
ui = (java/'client/greenhouse/BotanicalGreenhouseSeedBankScreen.java').read_text()
planner = (java/'farming/SeedBankSelection.java').read_text()
old_menu = (java/'content/BotanicalGreenhouseMenu.java').read_text()
journal = (java/'client/journal/MachineGuidePages.java').read_text()
# Regression checks also apply to all subsequent compatible Agri Evolution versions.
import re
version = re.search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', (root/'gradle.properties').read_text())
assert version and tuple(map(int, version.groups())) >= (0, 24, 0)
for token in ['SmartFarmingSeedDisabled','SmartFarmingSeedPriorities', 'SmartFarmingSeedReserve',
              'SmartFarmingPreferGenes','java.util.Arrays.fill(seedPriorities, 1)',
              'seedSlotEnabled(slot)', 'stack.getCount() <= reservedPerSlot',
              'GenomeStorage.fromSeed(stack).totalScore()',
              'return SeedBankSelection.chooseStrongest(eligible, seedSlotCursor, SEED_BANK_SIZE)',
              'tag.putIntArray("SmartFarmingSeedPriorities", seedPriorities)',
              'tag.getIntArray("SmartFarmingSeedPriorities")',
              'plantFlowerSeeds = !tag.contains("SmartFarmingFlowerFilter")',
              'plantResourceSeeds = !tag.contains("SmartFarmingResourceFilter")',
              'plant.setLineage(HybridLineageStorage.fromSeed(seed))',
              'plant.setRareTrait(RareTraitStorage.fromSeed(seed))']:
    assert token in be, f'missing entity feature: {token}'
for token in ['checkContainerDataCount(settings, 20)', 'addDataSlots(settings)',
              'greenhouse.toggleSeedSlot(button)','greenhouse.cycleSeedPriority(button - INPUTS)',
              'greenhouse.cycleSeedReserve()', 'greenhouse.toggleBestGenetics()']:
    assert token in menu, f'missing menu feature: {token}'
for token in ['Best genes: ', 'Reserve: ', 'menu.slotEnabled(i)', 'menu.priority(i)',
              'handleInventoryButtonClick(menu.containerId']:
    assert token in ui, f'missing GUI control: {token}'
assert 'candidate.priority() > bestPriority' in planner
assert 'Math.floorMod(candidate.slot() - startCursor, slotCount)' in planner
assert 'Wheat / Flowers:' in (java/'client/greenhouse/BotanicalGreenhouseScreen.java').read_text()
assert 'resourcePlantingEnabled()' in old_menu and 'flowerPlantingEnabled()' in old_menu
assert 'ADVANCED SEED BANK' in journal and 'SEED BANK RESERVES' in journal
for relative, sha in json.loads((root/'WORKING_TEXTURE_SHA256.json').read_text()).items():
    f = root/'src/main/resources/assets/agri_evolution'/relative
    assert hashlib.sha256(f.read_bytes()).hexdigest() == sha, f'unwanted sprite edit: {relative}'
print('PASS: Advanced Seed Bank config, defaults, server buttons, selector, filters, NBT, journal and approved textures')
