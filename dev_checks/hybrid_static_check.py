from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
assets=RES/'assets/agri_evolution'
errors=[]
count=0
for file in RES.rglob('*.json'):
    try: json.loads(file.read_text())
    except Exception as e: errors.append(f'Invalid JSON: {file}: {e}')
    count+=1
for species in ('eclipse_lotus','twilight_sage'):
    for path in [
        assets/f'models/item/{species}_seed.json',assets/f'textures/item/{species}_seed.png',
        assets/f'blockstates/{species}_crop.json',
        RES/f'data/agri_evolution/loot_table/blocks/{species}_crop.json']:
        if not path.is_file(): errors.append('Missing '+str(path))
    for i in range(8):
        for path in [assets/f'models/block/{species}_crop_stage{i}.json',assets/f'textures/block/{species}_crop_stage{i}.png']:
            if not path.is_file(): errors.append('Missing '+str(path))
    entry=json.loads((assets/f'blockstates/{species}_crop.json').read_text())
    assert any(x.get('when',{}).get('supported')=='true' for x in entry['multipart'])
    assert all(any(x.get('when',{}).get('age')==str(i) for x in entry['multipart']) for i in range(8))
# Scan model JSON for first-party file references
for p in list((assets/'models').rglob('*.json')) + list((assets/'blockstates').rglob('*.json')):
    data=json.loads(p.read_text())
    text=p.read_text()
    for ref in re.findall(r'"agri_evolution:(?:block|item)/[^"\s]+"',text):
        src=ref[1:-1]
        namespace, file=src.split(':')
        kind,name=file.split('/',1)
        if kind=='block':
            if p.parent.name in ('blockstates','item'): target=assets/f'models/block/{name}.json'
            else: target=assets/f'textures/block/{name}.png'
        else: target=assets/f'textures/item/{name}.png'
        if not target.exists(): errors.append('Missing reference '+str(p)+' -> '+str(target))
# ensure every new java enum referenced in necessary systems
for rel in ['content/AEItems.java','content/AEBlocks.java','content/AgriCropBlock.java','content/BotanicalGreenhouseBlockEntity.java','client/journal/CompactJournalRenderer.java','client/journal/JournalScreen.java']:
    txt=(ROOT/'src/main/java/com/agrievolution'/rel).read_text()
    for species in ('ECLIPSE_LOTUS','TWILIGHT_SAGE'):
        if species not in txt: errors.append('Missing '+species+' registration in '+rel)
if errors:
    print('\n'.join(errors[:70]));sys.exit(1)
print(f'PASS: {count} JSON assets/recipes/loot files parsed; all hybrid models, growth stages and Java species mappings present.')
