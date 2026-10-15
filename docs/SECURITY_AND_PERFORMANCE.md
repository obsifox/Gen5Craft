# Hardening & performance (Phase 5)

- All downloads (game data from Mojang, client updates from the manifest)
  are hash-verified (SHA-1 / SHA-256) and valid files are never re-downloaded.
- The launcher re-verifies the installed client jar against the installed
  manifest on Play and refuses to launch on mismatch.
- No credential is stored anywhere: GitHub access uses the Drive key in
  memory only (maintainer-side); the shipped apps contain no token.
- Online-mode servers stop at the encryption request: Microsoft account
  authentication is a user step, never faked or bypassed.
- Performance budgets come from `PerfProfile`: PERFORMANCE = 60% scale cap,
  30 FPS cap, static scene; BALANCED = 85%/60 FPS; QUALITY = 100%/user FPS.
  Panorama and 3D previews render into reused off-screen buffers at the
  configured render scale; UI tabs are created lazily.
