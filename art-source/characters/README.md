# Pine Creek Character Source

Character visuals are authored in Blender and exported to Godot.

Primary design specification: `docs/CHARACTERS.md`

Planned source layout:

- pine_human_base.blend
- protagonist.blend
- bob.blend
- jim.blend
- mayor.blend
- kevin.blend

Rules:

- Blender source is the visual source of truth.
- Game GLB and portrait PNGs are derived outputs.
- Do not use generated images as the canonical character reference.
- Human characters should share one compatible skeleton where practical.
- Keep mobile triangle/material budgets from `docs/CHARACTERS.md`.
