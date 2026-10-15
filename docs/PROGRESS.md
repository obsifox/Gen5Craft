# Gen5Craft Progress

Foundation roadmap: [####################] 100% (v1.0.0, hotfix v1.0.1)
Gameplay roadmap:   [############--------] 60% (after v1.3.0)

- [x] Phase 6 - Interactive local play session (world -> 3D session, camera
      rotate/zoom by keys, drag and wheel; GPU/quality aware) `v1.1.0`
- [x] Phase 7 - Live Gen-5 chunk-data decoding into the renderer (Level
      Chunk With Light packets -> ChunkColumns -> 3D view; chunk batches
      acknowledged, keep-alives echoed; 26.3 block-state table) `v1.2.0`
- [x] Phase 8 - Player physics (AABB collision on decoded chunks), entity
      tracking (Add/Remove Entity packets), inventory model + hotbar HUD `v1.3.0`
- [ ] Phase 9 - Online-mode Microsoft auth (owner credential step) <- NEXT
- [ ] Phase 10 - Public release polish (owner decision)

Remaining on the gameplay roadmap: 40% (Phases 9-10). Honest boundaries:
online-mode auth is a user step; block collision uses full cubes for now;
entity movement packets and server container sync are later slices;
public release is the owner's call.
