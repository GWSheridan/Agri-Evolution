#!/usr/bin/env python3
"""Static checks for v0.23.0. Runtime gameplay and Gradle compilation still required."""
from pathlib import Path
import hashlib
import json
root = Path(__file__).resolve().parents[1]
java = root/'src/main/java/com/agrievolution'
be = (java/'content/BotanicalGreenhouseBlockEntity.java').read_text()
menu = (java/'content/BotanicalGreenhouseMenu.java').read_text()
ui = (java/'client/greenhouse/BotanicalGreenhouseScreen.java').read_text()
selector = (java/'farming/SeedBankSelection.java').read_text()
helper = (java/'farming/SeedBankMonitoring.java').read_text()
# Regression checks also apply to all subsequent compatible Agri Evolution versions.
# Since 0.29.5 the existing 42 dashboard fields remain at indices 0-41,
# followed by six additional read-only genetic-monitoring fields (42-47).
import re
version = re.search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', (root/'gradle.properties').read_text())
assert version and tuple(map(int, version.groups())) >= (0, 24, 0)
for token in ['private int monitorAttempts;', 'private int monitorSuccessful;',
              'monitorAttempts >= 30000', 'recordPlantCycle(SeedBankMonitoring.PLANTED',
              'recordPlantCycle(SeedBankMonitoring.noEligibleReason(',
              'SeedBankMonitoring.NO_PLOTS', 'SeedBankMonitoring.PLACEMENT_FAILED',
              'tag.putInt("MonitoringAttempts"', 'tag.getInt("MonitoringAttempts")',
              'tag.putInt("MonitoringLastSpecies"', 'tag.getInt("MonitoringLastSpecies")',
              'case 41 -> seedBankSummary().filtered()', 'getCount() { return 51; }',
              'plant.setLineage(HybridLineageStorage.fromSeed(seed))',
              'plant.setRareTrait(RareTraitStorage.fromSeed(seed))']:
    assert token in be, token
for token in ['new SimpleContainerData(51)', 'checkContainerDataCount(data, 51)',
              'monitorResultLabel()', 'monitorEfficiency()', 'monitorSpeciesName()',
              'monitorFiltered()']:
    assert token in menu, token
for token in ['"Planting", "Monitor"', 'case 7 -> monitoring(g, x, y)',
              'menu.monitorSuccessful()', 'menu.monitorSkipped()', 'menu.monitorEligible()',
              'menu.monitorDisabled()', 'menu.monitorReserved()', 'menu.monitorFiltered()',
              'menu.harvestCount()', 'menu.replantCount()']:
    assert token in ui, token
assert 'handleInventoryButtonClick' not in ui[ui.index('private void monitoring('):ui.index('private static String clock(')]
assert 'return SeedBankSelection.chooseStrongest(eligible, seedSlotCursor, SEED_BANK_SIZE);' in be
assert 'candidate.priority() > bestPriority' in selector
assert 'record Summary(' in helper and 'int noEligibleReason(' in helper
assets = root/'src/main/resources/assets/agri_evolution'
for file, sha in json.loads((root/'WORKING_TEXTURE_SHA256.json').read_text()).items():
    assert hashlib.sha256((assets/file).read_bytes()).hexdigest() == sha, f'Approved art modified: {file}'
print('PASS: 0.23.0 synced monitor, seed-bank exclusions, counters, NBT, unchanged planner and approved sprites')
