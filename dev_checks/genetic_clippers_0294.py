#!/usr/bin/env python3
"""Offline integration guard; source/JSON checks, NOT a NeoForge compile."""
from pathlib import Path
import json
P=Path(__file__).resolve().parents[1]
J=P/'src/main/java/com/agrievolution'
A=P/'src/main/resources/assets/agri_evolution'
C=(J/'content/GeneticClippersItem.java').read_text()
B=(J/'content/AECropBlockEntity.java').read_text()
I=(J/'content/AEItems.java').read_text()
T=(J/'content/AECreativeTabs.java').read_text()
assert 'GENETIC_CLIPPERS' in I and 'GENETIC_CLIPPERS' in T
assert 'plant.hasBeenClipped()' in C and 'plant.setClipped(true)' in C
assert 'state.getValue(CropBlock.AGE) < crop.getMaxAge()' in C
assert 'GeneticSampleItem.capture(sourceSeed)' in C
assert 'GenomeStorage.putOnSeed(sourceSeed, plant.genome())' in C
assert 'RareTraitStorage.putOnSeed(sourceSeed, plant.rareTrait())' in C
assert 'HybridLineageStorage.putOnSeed(sourceSeed, plant.lineage())' in C
assert 'tag.putBoolean("AgriGeneticClipped", clipped)' in B
assert 'clipped = tag.getBoolean("AgriGeneticClipped")' in B
assert 'item.agri_evolution.genetic_clippers' in json.loads((A/'lang/en_us.json').read_text())
model=json.loads((A/'models/item/genetic_clippers.json').read_text())
assert model['textures']['layer0']=='agri_evolution:item/clipper'
assert (A/'textures/item/clipper.png').exists()
recipe=json.loads((P/'src/main/resources/data/agri_evolution/recipe/genetic_clippers.json').read_text())
assert recipe['result']['id']=='agri_evolution:genetic_clippers'
assert 'GENETIC CLIPPERS' in (J/'client/journal/MachineGuidePages.java').read_text()
print('PASS v0.29.4: non-destructive genetic clippers, once-per-planting persistence, source assets & journal')
