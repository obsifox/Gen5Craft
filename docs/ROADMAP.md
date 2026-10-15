# Gen5Craft Roadmap (autopilot order)

- [x] Phase 1 — Menu (panorama/RTL/glass), GPU settings, Gen-5 gating, Mojang manifest downloader. `v0.1.0`
- [x] Phase 2.1 — Gen-5 server status ping (VarInt protocol foundation, version/protocol detection). `v0.2.0`
- [x] Launcher — Windows launcher: manifest update check, SHA-256 verified client install, Play. `v0.3.0`
- [ ] Phase 2.2 — Extract registries/data from downloaded 26.3 data (first slice shipped in v0.3.0: manifest-level data summary written to Datas/) ← current
- [ ] Phase 2.2 — Extract registries/data from the downloaded official 26.3 data into Datas/
- [ ] Phase 2.3 — Offline local-world model (world list, create/load metadata) on latest data
- [ ] Phase 3 — CPU-software 3D renderer preview, switched by the GPU settings (Auto/Integrated/Dedicated/CPU)
- [ ] Phase 4 — Login/play protocol session for Gen-5 servers (online-mode auth boundary stops for owner input if credentials are ever required)
- [ ] Phase 5 — Hardening: installer, auto-update via gen5craft-manifest.json, performance passes for weak/strong PCs

Rule: one slice at a time; compile + tests + Windows CI + draft release + Drive report must be green before the next slice starts.
