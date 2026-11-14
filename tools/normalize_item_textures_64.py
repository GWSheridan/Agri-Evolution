from pathlib import Path
from PIL import Image
import json

ROOT = Path(__file__).resolve().parents[1] / 'src' / 'main' / 'resources' / 'assets' / 'agri_evolution'
MODELS = ROOT / 'models' / 'item'
TEXTURES = ROOT / 'textures' / 'item'

def binarize_alpha(img: Image.Image) -> Image.Image:
    img = img.convert('RGBA')
    pixels = []
    for r, g, b, a in img.getdata():
        pixels.append((r, g, b, 255 if a >= 128 else 0))
    img.putdata(pixels)
    return img

def normalize_to_64(path: Path) -> None:
    img = Image.open(path).convert('RGBA')
    if img.size != (64, 64):
        if img.width == img.height:
            img = img.resize((64, 64), Image.Resampling.NEAREST)
        else:
            bbox = img.getbbox()
            if bbox:
                img = img.crop(bbox)
            scale = 56 / max(img.width, img.height)
            size = (max(1, round(img.width * scale)), max(1, round(img.height * scale)))
            img = img.resize(size, Image.Resampling.NEAREST)
            canvas = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
            canvas.alpha_composite(img, ((64 - size[0]) // 2, (64 - size[1]) // 2))
            img = canvas
    img = binarize_alpha(img)
    img.save(path)

def main() -> None:
    item_ids = set()
    for file in MODELS.glob('*.json'):
        data = json.loads(file.read_text())
        layer0 = (data.get('textures') or {}).get('layer0')
        if isinstance(layer0, str) and layer0.startswith('agri_evolution:item/'):
            item_ids.add(layer0.split('/', 1)[1])
    for item_id in sorted(item_ids):
        path = TEXTURES / f'{item_id}.png'
        if path.exists():
            normalize_to_64(path)
            print(f'normalised {item_id}')

if __name__ == '__main__':
    main()
