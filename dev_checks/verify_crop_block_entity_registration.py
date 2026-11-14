#!/usr/bin/env python3
"""Offline Agri Evolution crop-block-entity registration regression test.

Only the Minecraft/NeoForge runtime can exercise actual block placement.  This
script catches the source-level omission that caused the 0.19.1 crash.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "main" / "java" / "com" / "agrievolution"


def main():
    species = (SRC / "genetics" / "CropSpecies.java").read_text(encoding="utf-8")
    blocks = (SRC / "content" / "AEBlocks.java").read_text(encoding="utf-8")
    entities = (SRC / "content" / "AEBlockEntities.java").read_text(encoding="utf-8")
    names = re.findall(
        r'^\s*([A-Z][A-Z0-9_]*)\("[^"]+",\s*"[^"]+",\s*"[^"]+",\s*"[^"]+",\s*(?:true|false)\)[,;]',
        species, re.M)
    expected = {'AGRI_CROP' if name == 'WILD' else f'{name}_CROP' for name in names}
    assert len(names) == len(expected) and len(names) >= 28, 'Unexpected species registry'
    definitions = re.findall(r'public static final DeferredBlock<AgriCropBlock>\s+([A-Z][A-Z0-9_]*)\s*=', blocks)
    type_match = re.search(r'AECropBlockEntity::new,\s*(.*?)\)\.build\(null\)', entities, re.S)
    assert type_match, 'Crop block entity registration not found'
    accepted = re.findall(r'AEBlocks\.([A-Z][A-Z0-9_]*)\.get\(\)', type_match.group(1))
    assert len(definitions) == len(set(definitions)), 'Duplicate crop declarations'
    assert len(accepted) == len(set(accepted)), 'Duplicate crop block entity entries'
    missing_defs = expected - set(definitions)
    extra_defs = set(definitions) - expected
    missing_allowed = expected - set(accepted)
    extra_allowed = set(accepted) - expected
    if any((missing_defs, extra_defs, missing_allowed, extra_allowed)):
        print(f'FAIL: Missing crop definitions={sorted(missing_defs)}, extra definitions={sorted(extra_defs)}')
        print(f'FAIL: Invalid block entities: missing={sorted(missing_allowed)}, unexpected={sorted(extra_allowed)}')
        return 1
    print(f'PASS: {len(names)} crop species, {len(definitions)} registered crop blocks, '
          f'{len(accepted)} accepted by the shared AECropBlockEntity')
    for name in ('GRAVEL', 'SAND', 'CLAY', 'FLINT', 'GLOWSTONE', 'OBSIDIAN'):
        assert f'{name}_CROP' in accepted
    print('PASS: all six new resource crops can create the registered block entity')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (AssertionError, OSError) as exc:
        print(f'FAIL: {exc}')
        sys.exit(1)
