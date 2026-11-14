from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'src' / 'main' / 'resources' / 'assets' / 'agri_evolution' / 'textures' / 'block'

def binarize_alpha(img: Image.Image) -> Image.Image:
    img = img.convert('RGBA')
    pixels = []
    for r, g, b, a in img.getdata():
        pixels.append((r, g, b, 255 if a >= 128 else 0))
    img.putdata(pixels)
    return img

def normalize(path: Path) -> None:
    img = Image.open(path).convert('RGBA')
    if img.size == (64, 64):
        out = binarize_alpha(img)
    elif img.width == img.height:
        out = binarize_alpha(img.resize((64, 64), Image.Resampling.NEAREST))
    else:
        bbox = img.getbbox()
        if bbox:
            img = img.crop(bbox)
        scale = min(64 / img.width, 64 / img.height)
        size = (max(1, round(img.width * scale)), max(1, round(img.height * scale)))
        scaled = img.resize(size, Image.Resampling.NEAREST)
        out = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        out.alpha_composite(scaled, ((64 - size[0]) // 2, (64 - size[1]) // 2))
        out = binarize_alpha(out)
    out.save(path)

def main() -> None:
    for path in sorted(ROOT.glob('*.png')):
        normalize(path)
        print(f'normalised {path.name}')

if __name__ == '__main__':
    main()
