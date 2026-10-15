# Gen5Craft — کلاینت نسل ۵ ماینکرافت (Java Edition)

A Windows-first Java client foundation for **Generation 5** of Minecraft Java
Edition (**1.20.5 → 26.3**, protocol 766+). The client is built on the latest
Gen-5 data (Mojang release **26.3**, 2026-09-15) and adapts down to older
Gen-5 servers by *disabling* content introduced after that server's version.
Local worlds/maps always run on the latest data and disable nothing.

## Main menu (as designed)
- Panorama background, pre-rendered and cached; static on weak systems.
- Small MINECRAFT logo bottom-left, version bottom-right.
- Fully transparent header; buttons right-to-left:
  **Language, Multiplayer, Singleplayer, Settings, Exit**.
- Each tab opens as a dark glass panel in the center over the panorama.
- Settings include a **GPU section**: Auto / Integrated (CPU graphics) /
  Dedicated GPU / CPU software rendering, plus render scale, FPS limit,
  panorama animation and an Auto performance profile for weak/strong systems.
- Persian (RTL) by default, English (LTR) included.

## Install like a client jar
Download `Gen5Craft.jar` (or `Gen5Craft-windows.zip`) from the GitHub release
of this repo, or use `gen5craft-manifest.json` which lists the files with
SHA-256 hashes. On first use, the Singleplayer tab can download the official
26.3 data (client jar, 114 libraries, asset index 34) through Mojang's own
version manifest, verifying every file by SHA-1 and skipping valid files.

Requires Java 25 on Windows (same as Minecraft 26.3 itself).

## Build locally
```powershell
javac --release 17 -encoding UTF-8 -d out (Get-ChildItem -Recurse src/main/java -Filter *.java).FullName
java -cp out com.gen5craft.Main
```
CI (GitHub Actions, `windows-latest`) compiles, tests, packages the jar and
the Windows app image, and publishes release assets ready to download.

## Status / roadmap
Phase 1 (v0.1.0): menu, GPU settings, Gen-5 version gating, manifest downloader.
Phase 2.1 (v0.2.0): Gen-5 server status ping (VarInt protocol
foundation, live version/protocol/players detection) in the Multiplayer tab.
Launcher (v0.3.0, this build): `Gen5Craft-Launcher.jar` /
`Gen5Craft-Launcher-windows.zip` — checks `gen5craft-manifest.json`, installs
the client jar with SHA-256 verification and launches it. The downloader now
also writes a Phase-2.2 data summary (`Datas/gen5-data-summary.json`). The generation model is fixed: crossing below 1.20.5 is a
different generation and is intentionally rejected, not emulated by disabling.
Later phases add the 3D engine and live server protocol on this foundation.

## Disclaimer
Unofficial fan project; not affiliated with Mojang or Microsoft. Game data is
downloaded from Mojang's official manifest at runtime by the user.


## Progress (text)
Weights: Phase 1 = 10, Ping 2.1 = 5, Launcher = 10, Data 2.2 = 10,
World model 2.3 = 10, 3D renderer Phase 3 = 25, Play protocol Phase 4 = 25,
Hardening Phase 5 = 5 (total 100). Roadmap completed at **v1.0.0 (100%)**. Install: see `docs/INSTALL.md`.
Hardening & performance notes: `docs/SECURITY_AND_PERFORMANCE.md`.
See `docs/PROGRESS.md` (regenerated every release).
