"""Check every seed sprite; deliberate separate kernels are allowed, stray dots are not."""
from pathlib import Path
from PIL import Image
import json

root = Path(__file__).resolve().parents[1]
folder = root / 'src/main/resources/assets/agri_evolution/textures/item'
records, problems = [], []
for file in sorted(folder.glob('*seed*.png')):
    if file.stem.startswith('seed_bag'):
        continue
    image = Image.open(file).convert('RGBA')
    alpha = image.getchannel('A')
    bbox = alpha.getbbox()
    errors = []
    if image.size != (64, 64): errors.append('size must be 64x64')
    if set(alpha.tobytes()) - {0, 255}: errors.append('partial alpha/halo')
    if not bbox: errors.append('empty sprite')
    elif bbox[0] == 0 or bbox[1] == 0 or bbox[2] == 64 or bbox[3] == 64: errors.append('touches canvas edge')
    pending = {(x, y) for y in range(image.height) for x in range(image.width) if alpha.getpixel((x, y))}
    tiny = []
    while pending:
        start = pending.pop()
        component, queue = [start], [start]
        while queue:
            x, y = queue.pop()
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    neighbor = (x+dx, y+dy)
                    if neighbor in pending:
                        pending.remove(neighbor)
                        component.append(neighbor)
                        queue.append(neighbor)
        if len(component) == 1: tiny.extend(component)
    if tiny: errors.append('isolated pixels: ' + str(tiny))
    records.append({'file': file.name, 'bbox': bbox, 'errors': errors})
    problems.extend(file.name + ': ' + error for error in errors)
output = root / 'build/audit-1.3.4'
output.mkdir(parents=True, exist_ok=True)
(output / 'seed-art-check.json').write_text(json.dumps(records, indent=2)+'\n', encoding='utf-8')
if problems:
    raise SystemExit('\n'.join(problems))
print(f'Verified {len(records)} seed icons: 64x64, hard transparency, no clipped edges or isolated pixels.')
