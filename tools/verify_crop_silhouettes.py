"""Guard against block-shaped new flowers in Agri Evolution 0.5.1."""
from pathlib import Path
from PIL import Image
import hashlib

root = Path(__file__).resolve().parents[1]
block = root/'src/main/resources/assets/agri_evolution/textures/block'
new = ['amber_marigold', 'silver_thistle', 'sunfire_lily', 'moon_orchid']
for species in new:
    hashes = []
    mature_count = None
    for age in range(8):
        path = block/f'{species}_crop_stage{age}.png'
        rgba = Image.open(path).convert('RGBA')
        assert rgba.size == (16,16), path
        alpha = rgba.getchannel('A')
        pixels = list(alpha.getdata())
        assert set(pixels) <= {0,255}, f'Partially transparent pixels: {path.name}'
        count = sum(p > 0 for p in pixels)
        box = alpha.getbbox()
        assert box is not None and 4 <= count <= 85 and box[1] >= 2, (path.name,count,box)
        hashes.append(hashlib.sha256(path.read_bytes()).hexdigest())
        if age == 7: mature_count = count
    assert len(set(hashes)) == 8, f'Duplicate growth ages for {species}'
    print(f'{species}: 8/8 distinct transparent stages, {mature_count} mature pixels')
print('OK: all 32 improved crop sprites match the slender silhouette limits.')
