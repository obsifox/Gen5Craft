# Install Gen5Craft (Windows)

Option A (recommended): download `Gen5Craft-Launcher-windows.zip`, unzip,
run `Install-Gen5Craft.ps1` from that folder (or start `Gen5CraftLauncher.exe`
directly), then in the launcher press **Check / Update client** and **Play**.
The Windows app images bundle their Java runtime; the standalone jars need
Java 25 (same as Minecraft 26.3).

Option B: `java -jar Gen5Craft-Launcher.jar` (or `Gen5Craft.jar` directly).

The launcher verifies the client jar with SHA-256 from
`gen5craft-manifest.json` before launching and refuses to start a corrupted
or tampered jar. Game data lives in `%APPDATA%\Gen5Craft`.
