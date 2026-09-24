# Blender Character Tools

This directory contains repeatable Blender scripts for Pine Creek character production.

Current:

- `build_character_prototypes.py` — generates the shared humanoid skeleton, protagonist/Bob v1 GLBs, source `.blend` files, portrait PNGs and comparison preview

Next planned split after the prototype art direction is approved:

- build_human_base.py — common low-poly body, rig and modular sockets
- build_protagonist.py / build_bob.py / build_jim.py / build_mayor.py / build_kevin.py
- export_characters.py — GLB export
- render_portraits.py — consistent transparent portrait renders

Linux Blender: `/home/festice/.local/bin/blender`

Character design source of truth: `docs/CHARACTERS.md`.
