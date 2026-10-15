---
name: gen5craft-autopilot
description: Autonomous maximum-quality execution skill for the Gen5Craft project (Generation-5 Minecraft Java client, 1.20.5-26.3, Windows/Java). Use whenever work continues on Gen5Craft, rMinecraft/RCraft successors, or any Gen-5 client slice. Proceeds phase by phase, one slice at a time, WITHOUT asking the user for routine approval.
version: 1.0.0
owner-project: Gen5Craft
repository: https://github.com/obsifox/Gen5Craft (private)
drive-folder: Gen5Craft/ (source, Version, Datas, Logs)
---

# Gen5Craft Autopilot — اسکیل اجرای خودکار با حداکثر کیفیت

## Mission
Build Gen5Craft as a Windows-first Java client for Minecraft Java **Generation 5
(1.20.5 -> 26.3, protocol 766+)**, based on the latest official data (26.3),
which adapts down to older Gen-5 servers by disabling newer content. Local
worlds always run on the latest data. Crossing below 1.20.5 is a different
generation: never emulate it by "disabling"; reject it explicitly.

## Non-negotiable rules (from the owner, override convenience)
1. **Workspace stays empty.** Nothing is kept in /home/user or the agent's own
   space. Work happens in memory or an ephemeral /tmp dir that is DELETED
   immediately after the push/upload completes; verify emptiness before reporting.
2. **Storage only in GitHub + Google Drive**, under the project's own name:
   repo `obsifox/Gen5Craft` and Drive folder `Gen5Craft/` with
   `source/ Version/ Datas/ Logs/`. No other location, no renamed side-copies.
3. **GitHub key comes from Google Drive** (`key.md`). Use it in memory only:
   never write it to a file, git config, remote URL, code, log, or reply.
4. No file named RULES is maintained; this SKILL plus the chat rules are the
   operating contract.

## Autonomy contract (no routine approval needed)
Proceed WITHOUT asking for: reading/research, code, tests, refactors,
documentation, GitHub commits/pushes to Gen5Craft, CI runs and fixing them,
draft/private releases, Drive reports, creating/updating GitHub issues,
performance tuning, and starting the next roadmap slice.
STOP and report (do not proceed) ONLY for: making repo/release public,
deleting existing user data/artifacts/releases, rotating/revoking credentials,
spending money, changing the generation boundary, or a destructive rewrite
that discards working phases. Everything else: decide, execute, verify, report.

## Maximum-mode execution loop (run for every slice, one by one)
1. **Inspect** — repo/CI/Drive state, latest official Mojang manifest, open issues.
2. **Slice** — pick the single highest-value next slice from docs/ROADMAP.md;
   keep it small enough to compile, test and ship in one turn.
3. **Build in /tmp** — clone/pull, implement with zero unnecessary dependencies,
   optimized for weak AND strong systems (pre-render/cache, cap FPS, no
   per-frame allocation, lazy tab/module creation, async network/disk).
4. **Verify locally** — compile + dependency-free tests must pass before push.
5. **Ship** — push with the in-memory Drive key; wait for GitHub Actions
   (windows-latest, Java 25); if red, read logs, fix, re-push (loop, max 5).
6. **Publish download** — draft Release assets (artifact quota is full, so
   never rely on workflow artifacts) + `gen5craft-manifest.json` with SHA-256.
7. **Report to Drive** — build report to `Logs/`, download links to `Version/`,
   new game data to `Datas/`, docs/skill to `source/`.
8. **Clean + confirm** — delete /tmp workdir, prove workspace empty, then
   report links, hashes, test results and the NEXT slice; continue to it in
   the following turn without waiting for approval.

## Quality gates (all must be green before calling a slice done)
Compile on Java 25 CI · tests pass · main menu matches the approved design
(panorama, small logo bottom-left, version bottom-right, transparent RTL
header: Language/Multiplayer/Singleplayer/Settings/Exit, dark glass center) ·
GPU settings persist (Auto/Integrated/Dedicated/CPU-software + auto profile) ·
generation gating correct (Gen 5 only) · SHA-256 manifest regenerated ·
README/roadmap updated · no secret in repo (`key`, `token`, `github_pat` scan).

## Roadmap order (see docs/ROADMAP.md)
Phase 1 done (menu/settings/gating/downloader). Then: 2.1 server-status ping
-> 2.2 registry/data extraction from downloaded 26.3 data -> 2.3 offline/local
world model -> 3.x software 3D renderer honoring GPU modes -> 4.x login/play
protocol for Gen-5 servers -> 5.x hardening, installer, performance passes.
Never skip a gate to jump ahead; finish, ship and report each slice first.

## Communication
Report in Persian, short and factual: what shipped, where (links), hashes,
test/CI result, cleanup proof, and what slice runs next. State limits plainly
(phase vs. full client); never claim a phase is the finished game.
