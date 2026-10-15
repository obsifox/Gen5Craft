package com.gen5craft.launcher;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Local account profile (owner decision, Phase 9 revised): Gen5Craft uses a
 *  LOCAL profile instead of Microsoft online authentication. The profile is
 *  a name (the offline-mode identity), a skin (default Steve or a custom
 *  64x64 PNG) and a cosmetic cape. It is stored as profile.json in the game
 *  folder, shown on the launcher's profile card, and never leaves the PC.
 *  Online-mode servers still stop at the encryption boundary - a local
 *  profile is not a Mojang/Microsoft login and is never presented as one. */
public final class Profile {
    public enum Cape {
        NONE("No cape", 0, 0, 0),
        CRIMSON("Crimson cape", 150, 30, 40),
        AZURE("Azure cape", 40, 90, 180),
        EMERALD("Emerald cape", 30, 140, 70),
        GOLD("Golden cape", 200, 160, 40),
        VIOLET("Violet cape", 110, 60, 170);
        public final String label;
        public final Color color;
        Cape(String label, int r, int g, int b) { this.label = label; this.color = new Color(r, g, b); }
        public static Cape byName(String n) {
            for (Cape c : values()) if (c.name().equals(n)) return c;
            return NONE;
        }
    }

    public String name = "Steve";
    /** "" = default Steve; otherwise a path to a custom 64x64 skin PNG. */
    public String skinPath = "";
    public Cape cape = Cape.NONE;

    /** Mojang-style name rule: 3-16 chars of [A-Za-z0-9_]. Returns null when invalid. */
    public static String sanitize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.length() < 3 || s.length() > 16) return null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')) return null;
        }
        return s;
    }

    /** The offline-mode UUID (version 3), identical to the client's LoginSession. */
    public UUID offlineUuid() {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public String toJson() {
        return "{\n  \"account\": \"local\",\n  \"name\": \"" + esc(name) + "\",\n  \"skin\": \"" + esc(skinPath)
                + "\",\n  \"cape\": \"" + cape.name() + "\"\n}\n";
    }
    public static Profile fromJson(String json) {
        Profile p = new Profile();
        String n = extract(json, "\"name\": \"");
        if (sanitize(n) != null) p.name = n;
        String skin = extract(json, "\"skin\": \"");
        if (skin != null) p.skinPath = skin;
        String cape = extract(json, "\"cape\": \"");
        if (cape != null) p.cape = Cape.byName(cape);
        return p;
    }
    public void save(Path file) throws Exception {
        Files.createDirectories(file.getParent());
        Files.write(file, toJson().getBytes(StandardCharsets.UTF_8));
    }
    public static Profile load(Path file) {
        try {
            if (Files.exists(file)) return fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        return new Profile();
    }
    /** The name the client pre-fills from a profile file (null when absent). */
    public static String readName(Path file) {
        try {
            if (Files.exists(file)) {
                Profile p = fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
                return p.name;
            }
        } catch (Exception ignored) {}
        return null;
    }
    private static String extract(String json, String key) {
        if (json == null) return null;
        int k = json.indexOf(key); if (k < 0) return null;
        int s = k + key.length(); int e = json.indexOf('"', s);
        return e < 0 ? null : json.substring(s, e);
    }
    private static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
