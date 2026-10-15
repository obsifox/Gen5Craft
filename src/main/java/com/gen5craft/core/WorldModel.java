package com.gen5craft.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Phase 2.3: offline local-world model.
 *  A world is a folder under saves/ with a small gen5craft-world.json:
 *  name, creation time, and the data version it runs on. Local worlds ALWAYS
 *  use the latest Gen-5 data (26.3) — nothing is version-disabled locally. */
public final class WorldModel {
    public final String name;
    public final long createdEpochMs;
    public final String dataVersion;
    public WorldModel(String name, long createdEpochMs, String dataVersion) {
        this.name = name; this.createdEpochMs = createdEpochMs; this.dataVersion = dataVersion;
    }

    public static WorldModel create(Path savesDir, String name) throws Exception {
        String safe = name == null ? "" : name.trim().replaceAll("[^A-Za-z0-9 _-]", "_");
        if (safe.isEmpty()) safe = "World";
        Path dir = savesDir.resolve(safe);
        int n = 2;
        while (Files.exists(dir)) dir = savesDir.resolve(safe + "-" + (n++));
        Files.createDirectories(dir);
        WorldModel w = new WorldModel(dir.getFileName().toString(), System.currentTimeMillis(), Gen5Versions.latest());
        Files.write(dir.resolve("gen5craft-world.json"), w.toJson().getBytes(StandardCharsets.UTF_8));
        return w;
    }

    public static List<WorldModel> list(Path savesDir) throws Exception {
        List<WorldModel> worlds = new ArrayList<WorldModel>();
        if (!Files.isDirectory(savesDir)) return worlds;
        DirectoryStream<Path> ds = Files.newDirectoryStream(savesDir);
        try {
            for (Path dir : ds) {
                if (!Files.isDirectory(dir)) continue;
                Path meta = dir.resolve("gen5craft-world.json");
                if (Files.exists(meta)) worlds.add(fromJson(new String(Files.readAllBytes(meta), StandardCharsets.UTF_8), dir.getFileName().toString()));
                else worlds.add(new WorldModel(dir.getFileName().toString(), 0L, Gen5Versions.latest()));
            }
        } finally { ds.close(); }
        return worlds;
    }

    public String toJson() {
        return "{\n  \"name\": \"" + esc(name) + "\",\n  \"createdEpochMs\": " + createdEpochMs
             + ",\n  \"dataVersion\": \"" + dataVersion + "\",\n  \"generation\": 5\n}\n";
    }
    public static WorldModel fromJson(String json, String fallbackName) {
        String name = extract(json, "\"name\": \"", fallbackName);
        String ver = extract(json, "\"dataVersion\": \"", Gen5Versions.latest());
        long created = 0L;
        String num = extractRaw(json, "\"createdEpochMs\": ");
        if (num != null) try { created = Long.parseLong(num.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
        return new WorldModel(name, created, ver);
    }
    private static String extract(String json, String key, String fallback) {
        int k = json.indexOf(key); if (k < 0) return fallback;
        int s = k + key.length(); int e = json.indexOf('"', s);
        return e < 0 ? fallback : json.substring(s, e);
    }
    private static String extractRaw(String json, String key) {
        int k = json.indexOf(key); if (k < 0) return null;
        int s = k + key.length(); int e = s;
        while (e < json.length() && (Character.isDigit(json.charAt(e)))) e++;
        return json.substring(s, e);
    }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
