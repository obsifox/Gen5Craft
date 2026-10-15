# Gen5Craft Roadmap 2 - Gameplay (after the 100% foundation roadmap)

- [x] Phase 6 — Interactive local play session: a world opens in the 3D
      software renderer with camera controls (A/D/arrows or mouse drag to
      rotate, W/S/arrows or wheel to zoom), honoring GPU/quality settings. `v1.1.0`
- [x] Phase 7 — Live Gen-5 chunk-data decoding into the renderer: the play
      session decodes Clientbound Level Chunk With Light packets (paletted
      containers: single / indirect / direct, heightmaps, block entities,
      light data; wire differences for protocols 766-769, 770-774 and
      775+ handled) into ChunkColumns, acknowledges chunk batches, echoes
      keep-alives, and renders the last decoded chunk from its real block
      states (26.3 state table pinned from the official server jar). `v1.2.0`
- [x] Phase 8 — Player physics, entities and inventory basics: an AABB
      player physics engine (gravity, jumping, axis-separated collision)
      that runs on decoded chunk data; live entity tracking from Add
      Entity / Remove Entities packets; and a 36-slot inventory model with
      vanilla stacking and a hotbar HUD (keys 1-9). `v1.3.0`
- [ ] Phase 9 — Online-mode Microsoft authentication (owner credential step;
      never automated or faked) <- NEXT
- [ ] Phase 10 — Public release polish (owner decision only)

Gameplay roadmap weights: each phase 20%. After v1.3.0: **60%**.

Honest boundaries after Phase 8: physics treats every non-fluid block as a
full cube (slab/stair shapes later); entity *movement* packets are not
applied yet (spawn/remove tracked, positions from spawn); the inventory is
a model + HUD — server container-content synchronization fills it in a
later slice, contents are never faked; first-person control of the physics
player inside the 3D view is a later slice (the orbit camera remains).
Block-state naming/coloring is pinned for 26.3; older Gen-5 servers decode
exact state IDs and heights with elevation-based colors. Reading real
saved worlds (Anvil region files) is also a later slice.
