"""Agri Evolution 0.32.2 - auditable, read-only texture catalog.

Run from the project: python tools/audit_texture_catalog_0322.py
Requires Pillow for PNG checks; Gradle verifyTextureCatalog0322 does not require Python.
"""
from collections import Counter, defaultdict
from hashlib import sha256
from pathlib import Path
from PIL import Image
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/agri_evolution"
TEXTURES = ASSETS / "textures"
OUT = ROOT / "TEXTURE_CATALOG_0.32.2.json"


def main():
    manifest = []
    errors = []
    for f in sorted(TEXTURES.rglob("*.png")):
        path = f.relative_to(TEXTURES).as_posix()
        category = path.split("/", 1)[0]
        with Image.open(f) as src:
            src.verify()
        with Image.open(f) as src:
            im = src.convert("RGBA")
            w, h = im.size
            alpha = im.getchannel("A")
            partial_alpha = any(0 < a < 255 for a in set(alpha.getdata()))
        if category in {"block", "item", "seed", "plant"}:
            if w != 64 or h < 64 or h % 64:
                errors.append(f"Wrong 64px frame size: {path} ({w}x{h})")
            if category in {"block", "seed"} and h != 64:
                errors.append(f"Expected standalone 64px tile: {path} ({w}x{h})")
            if category in {"item", "seed"} and h == 64 and partial_alpha:
                errors.append(f"Partial-alpha halo on standalone item: {path}")
            classification = "64px frame strip" if h > 64 else "64px standalone"
        else:
            classification = "layout/UV-sensitive legacy exception"
        manifest.append({
            "file": path,
            "width": w, "height": h,
            "classification": classification,
            "frames": h // 64 if classification in {"64px frame strip", "64px standalone"} else None,
            "sha256": sha256(f.read_bytes()).hexdigest(),
        })

    refs = []
    for model in sorted((ASSETS / "models").rglob("*.json")):
        try:
            data = json.loads(model.read_text(encoding="utf-8"))
        except Exception as exc:
            errors.append(f"Unreadable model JSON: {model.name}: {exc}")
            continue
        for tex in (data.get("textures") or {}).values():
            if isinstance(tex, str) and tex.startswith("agri_evolution:"):
                path = tex[len("agri_evolution:"):]
                refs.append({"model": model.relative_to(ASSETS).as_posix(), "texture": path})
                if not (TEXTURES / (path + ".png")).is_file():
                    errors.append(f"Model texture missing: {model.name} => {tex}")
    baseline = json.loads((ROOT / "WORKING_TEXTURE_SHA256.json").read_text(encoding="utf-8"))
    for p, expected in baseline.items():
        file = ASSETS / p
        if not file.is_file() or sha256(file.read_bytes()).hexdigest() != expected:
            errors.append(f"Known-good texture checksum mismatch: {p}")

    counts = Counter(entry["classification"] for entry in manifest)
    cats = Counter(entry["file"].split("/", 1)[0] for entry in manifest)
    output = {
        "release": "0.32.2", "total_png": len(manifest),
        "counts_by_category": dict(sorted(cats.items())),
        "counts_by_classification": dict(sorted(counts.items())),
        "models_scanned": len(list((ASSETS / "models").rglob("*.json"))),
        "model_texture_references_checked": len(refs),
        "baseline_checksums_checked": len(baseline),
        "errors": errors,
        "textures": manifest,
    }
    OUT.write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")
    print(f"PNG assets: {len(manifest)}; by category: {dict(cats)}")
    print(f"Classification: {dict(counts)}")
    print(f"References: {len(refs)}; baseline hashes: {len(baseline)}")
    print(f"ERRORS: {len(errors)}")
    for err in errors[:30]:
        print(" -", err)
    if errors:
        raise SystemExit(1)

if __name__ == "__main__":
    main()
