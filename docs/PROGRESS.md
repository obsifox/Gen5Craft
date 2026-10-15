# Gen5Craft Progress

Foundation roadmap: [####################] 100% (v1.0.0, hotfix v1.0.1)
Gameplay roadmap:   [####################] 100% (after v1.5.0)

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
- [x] Phase 10 - Public release polish: full release notes, public
      README/install flow, final version v1.5.0 published publicly on
      GitHub (repository + release) by owner decision `v1.5.0`

Post-roadmap maintenance: v1.6.0 - automatic first-run game-data
download in the client (progress + retry), one-click install-then-play in
the launcher, custom cape PNG import with a 3D character preview
(head + torso + cape) on the profile card.

Post-roadmap maintenance: v1.7.0 - fixed a launcher startup crash from
v1.6.0 (missing cape-card widget threw during window creation; the card is
restored and null-safe, and startup failures now show a dialog and write a
*-startup.log), modernized both UIs (rounded hover buttons, Segoe UI,
brand header), and embedded the Gen5Craft icon inside both jars and the
Windows executables (installer shortcut uses it too).

Remaining on the gameplay roadmap: 0% — both roadmaps complete.
Honest boundaries that remain true after v1.5.0: online-mode servers still
require real Microsoft authentication, which the local profile does not
provide or imitate; block collision uses full cubes for now; entity
movement packets and server container sync are later slices; the physics
player is not yet first-person controlled (orbit camera remains).
