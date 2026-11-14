#!/usr/bin/env python3
from pathlib import Path
from PIL import Image
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/agri_evolution'
for tier in ('cultivated_farmland','nutrient_farmland','mythic_farmland'):
    for moisture in ('dry','wet'):
        key=f'{tier}_{moisture}'
        data=json.loads((root/'models/block'/f'{key}.json').read_text())
        assert data['render_type']=='minecraft:cutout'
        assert data['textures']['top']==('minecraft:block/farmland' if moisture=='dry' else 'minecraft:block/farmland_moist')
        assert data['textures']['side']=='minecraft:block/dirt'
        assert data['textures']['accent']==f'agri_evolution:block/{key}'
        assert data['elements'][0]['to']==[16,15,16]
        assert data['elements'][1]['faces']['up']['texture']=='#accent'
        im=Image.open(root/'textures/block'/f'{key}.png').convert('RGBA')
        assert im.size==(16,16)
        alpha={p[3] for p in im.getdata()}
        assert alpha=={0,255},(key,alpha)
        assert 0.18 < sum(p[3]==255 for p in im.getdata())/256 < 0.50,key
    assert (root/'textures/block'/f'{tier}_side.png').is_file()
print('Natural farmland 0.26.1: all six wet/dry overlays and models verified')
