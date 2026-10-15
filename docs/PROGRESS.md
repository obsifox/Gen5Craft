# Gen5Craft Progress

Foundation roadmap: [####################] 100% (v1.0.0, hotfix v1.0.1)
Gameplay roadmap:   [################----] 80% (after v1.4.0)

- [x] Phase 6 - Interactive local play session (world -> 3D session, camera
      rotate/zoom by keys, drag and wheel; GPU/quality aware) `v1.1.0`
- [x] Phase 7 - Live Gen-5 chunk-data decoding into the renderer (Level
      Chunk With Light packets -> ChunkColumns -> 3D view; chunk batches
      acknowledged, keep-alives echoed; 26.3 block-state table) `v1.2.0`
- [x] Phase 8 - Player physics (AABB collision on decoded chunks), entity
      tracking (Add/Remove Entity packets), inventory model + hotbar HUD `v1.3.0`
- [x] Phase 9 - Local launcher profile: profile card with the default Steve
      head rendered from the game's own downloaded data (or a custom skin
      PNG), editable profile name, cosmetic cape, offline UUID, saved as
      profile.json and used as the client username. Local account only —
      not a Microsoft/Mojang login (owner-directed scope change) `v1.4.0`
- [ ] Phase 10 - Public release polish (owner decision) <- NEXT

Remaining on the gameplay roadmap: 20% (Phase 10 only). Honest boundaries:
online-mode servers still require real Microsoft authentication, which the
local profile does not provide or imitate; block collision uses full cubes
for now; entity movement packets and server container sync are later
slices; public release is the owner's call.
