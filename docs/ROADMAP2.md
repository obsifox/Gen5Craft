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
- [ ] Phase 8 — Player physics, entities and inventory basics <- NEXT
- [ ] Phase 9 — Online-mode Microsoft authentication (owner credential step;
      never automated or faked)
- [ ] Phase 10 — Public release polish (owner decision only)

Gameplay roadmap weights: each phase 20%. After v1.2.0: **40%**.

Honest boundaries after Phase 7: chunk decoding is wired for live servers
(packet IDs pinned per protocol 766-777); block-state *naming/coloring* uses
the pinned 26.3 table, while older Gen-5 servers still decode exact state
IDs and heights with elevation-based colors until their tables are pinned.
Reading real saved worlds (Anvil region files) is a later slice, as are
player physics/entities (Phase 8).
