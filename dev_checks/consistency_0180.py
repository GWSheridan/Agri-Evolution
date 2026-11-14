"""No Gradle required: check source registration and machine guide/inheritance wiring."""
from pathlib import Path
import re
r=Path(__file__).resolve().parents[1]/'src/main/java/com/agrievolution'
read=lambda f:(r/f).read_text()
cre=read('content/AECreativeTabs.java')
items=['WILD_SEED','CRIMSON_CLOVER_SEED','AZURE_FLAX_SEED','VIOLET_STARFLOWER_SEED','AMBER_MARIGOLD_SEED','SILVER_THISTLE_SEED','SUNFIRE_LILY_SEED','MOON_ORCHID_SEED','ECLIPSE_LOTUS_SEED','TWILIGHT_SAGE_SEED','DIRT_SEED','WOOD_SEED','STONE_SEED','COAL_SEED','COPPER_SEED','IRON_SEED','REDSTONE_SEED','GOLD_SEED','LAPIS_SEED','DIAMOND_SEED','EMERALD_SEED','QUARTZ_SEED','GRAVEL_SEED','SAND_SEED','CLAY_SEED','FLINT_SEED','GLOWSTONE_SEED','OBSIDIAN_SEED']
for item in items:
    assert f'output.accept(AEItems.{item}.get())' in cre, f'Missing creative seed {item}'
for f in ['content/BotanicalGeneticsLaboratoryBlockEntity.java','content/SeedBreedingStationBlockEntity.java','content/BreedingJournalItem.java']:
    assert 'HybridLineageStorage.putOnSeed' in read(f), f'Hybrid offspring not tagged in {f}'
for f in ['content/SpeciesSeedItem.java','content/AgriCropBlock.java','content/BotanicalGreenhouseBlockEntity.java','content/AECropBlockEntity.java']:
    assert 'HybridLineageStorage' in read(f), f'Hybrid ancestry not propagated in {f}'
for f in ['client/journal/JournalScreen.java','client/journal/CompactJournalRenderer.java']:
    assert 'MACHINES' in read(f), f'Machines missing from {f}'
assert read('client/journal/MachineGuidePages.java').count('new Page("') >= 22
print('PASS: 28 original creative seeds, lineage persistence and 22 instruction pages')
