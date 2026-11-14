#!/usr/bin/env python3
"""Static regression for source interactions; compile and in-game tests also required."""
from pathlib import Path
import struct
import json
p=Path(__file__).resolve().parents[1]
java=p/'src/main/java/com/agrievolution'
be=(java/'content/BotanicalGreenhouseBlockEntity.java').read_text()
assert 'if (!autoReplant || smartFarmingUpgraded) drops.add(seed);' in be
assert 'distributeDrops(drops, futureBank, futureStorage)' in be
assert 'installInventories(futureBank, futureStorage)' in be
assert 'int slot = chooseSeedBankSlot();' in be
assert 'placeBankSeed(server, cropPos, slot, supported)' in be
assert 'SeedBankSelection.chooseStrongest' in be
assert 'if (autoReplant)' in be and 'if (!smartFarmingUpgraded)' in be
assert 'if (ItemStack.isSameItemSameComponents(current, remaining)' in be
assert 'GenomeStorage.fromSeed(remaining).totalScore() > weakestScore' in be
assert 'insert(check, displaced)' in be
assert 'recycleStoredSeeds();' in be and 'futureStorage.set(slot, ItemStack.EMPTY)' in be
assert 'storeAll(List.of(offspring))' not in be
assert 'distributeDrops(List.of(offspring)' in be
starter=(java/'content/StarterProgressionEvents.java').read_text()
assert 'BlockDropsEvent' in starter and 'Items.WHEAT_SEEDS' in starter
assert 'getDrops().removeIf' in starter and 'Items.WHEAT_SEEDS' in starter
assert 'player.getPersistentData().putBoolean(RECEIVED, true)' in starter
assert 'AEItems.BREEDING_JOURNAL.get()' in starter
assert 'player.getInventory().add(journal)' in starter
assets=p/'src/main/resources/assets/agri_evolution/textures/item'
for f in ('breeding_journal.png','clipper.png','wild_seed.png','soil_sample.png'):
    data=(assets/f).read_bytes()
    assert data[:8] == bytes([137,80,78,71,13,10,26,10])
    assert struct.unpack('>II',data[16:24]) == (64,64)
    assert data[25] == 6  # RGBA truecolour
journal=(java/'client/journal/MachineGuidePages.java').read_text()
assert 'Vanilla wheat seeds are' in journal and 'bank before storage.' in journal
print('PASS: greenhouse seed routing, genetic priority, replanting, grass loot, starter journal, original 64px assets')
