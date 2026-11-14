#!/usr/bin/env python3
"""Agri Evolution v0.29.1 static checks; Minecraft runtime compile still required."""
from pathlib import Path
import hashlib,json
from PIL import Image
P=Path(__file__).resolve().parents[1]
J=P/'src/main/java/com/agrievolution';A=P/'src/main/resources/assets/agri_evolution';D=P/'src/main/resources/data/agri_evolution/recipe'
assert tuple(map(int, __import__('re').search(r'(?m)^mod_version=(\d+)\.(\d+)\.(\d+)$', (P/'gradle.properties').read_text()).groups())) >= (0, 29, 1)
assert 'stacksTo(64)' in (J/'content/AEItems.java').read_text()
s=(J/'content/SoilSpecimenItem.java').read_text()
r=(J/'farming/SoilResearch.java').read_text()
assert 'new CustomModelData(visual(kind))' in s and '"Origin"' in s
assert 'AgriPendingSpecimens' in r and 'pending.putInt(kind, pending.getInt(kind) - 1)' in r
assert 'level.dimension().location() + ":" + pos.asLong()' in r
assert 'SOIL_RESEARCH_REFINEMENT_UPGRADE' in (J/'content/SoilResearchTableBlock.java').read_text()
assert 'SOIL_RESEARCH_MASTERY_UPGRADE' in (J/'content/AEItems.java').read_text()
assert any(f'new SimpleContainerData({n})' in (J/'content/SoilResearchTableMenu.java').read_text() for n in (3, 4))
assert 'public void setStatus(int value)' in (J/'content/SoilResearchTableBlockEntity.java').read_text()
assert 'drawWordWrap' in (J/'client/soil/SoilResearchTableScreen.java').read_text()
assert 'message(player,' not in (J/'content/SoilResearchTableBlockEntity.java').read_text()
assert 'UPGRADE = IntegerProperty' in (J/'content/IrrigationBasinBlock.java').read_text()
assert 'server.setBlock(worldPosition' in (J/'content/IrrigationBasinBlockEntity.java').read_text()
variants=json.loads((A/'blockstates/irrigation_basin.json').read_text())['variants'];assert len(variants)==6
for key,obj in variants.items():
 assert (A/'models/block'/(obj['model'].split('/')[-1]+'.json')).exists()
for v in ('soil_research_refinement_upgrade','soil_research_mastery_upgrade'):
 assert v.upper() in (J/'content/AECreativeTabs.java').read_text()
 assert (A/'textures/item'/(v+'.png')).exists()
 assert (A/'models/item'/(v+'.json')).exists()
 assert json.loads((D/(v+'.json')).read_text())['result']['id']=='agri_evolution:'+v
    
kinds=['dirt','grass','coarse','rooted','podzol','farmland','cultivated','nutrient','mythic','arcane','celestial']
model=json.loads((A/'models/item/soil_specimen.json').read_text());assert len(model['overrides'])==len(kinds)
hashes=[]
for kind in kinds:
 name='soil_specimen_'+kind
 f=A/'textures/item'/(name+'.png');im=Image.open(f);assert im.size==(32,32);im.verify()
 hashes.append(hashlib.sha256(f.read_bytes()).hexdigest())
 assert (A/'models/item'/(name+'.json')).exists()
assert len(set(hashes))==11
for n in ['irrigation_wood_mk2','irrigation_wood_mk3']:
 f=A/'textures/block'/(n+'.png');assert Image.open(f).size==(16,16)
for rel,h in json.loads((P/'WORKING_TEXTURE_SHA256.json').read_text()).items():
 assert hashlib.sha256((A/rel).read_bytes()).hexdigest()==h,rel
# All asset/model JSON must be well formed
for f in A.rglob('*.json'): json.loads(f.read_text())
for f in D.glob('*.json'): json.loads(f.read_text())
print('PASS 0.29.1 source/assets: stacking, legacy specimen provenance, table GUI, module upgrades, 11 icons, 3 basin appearances and protected old textures')
