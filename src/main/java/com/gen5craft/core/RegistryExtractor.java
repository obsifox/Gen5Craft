package com.gen5craft.core;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Phase 2.2: indexes the *actual* downloaded official client jar.
 *  It records entry names and category counts only (blockstates, models,
 *  recipes, loot tables, tags, textures, sounds, ...) — a truthful index of
 *  what Mojang shipped, never invented registry content. The full name list
 *  is written next to the game data; the repo keeps a compact summary. */
public final class RegistryExtractor {
    public static final class Index {
        public int totalEntries;
        public final Map<String, Integer> categories = new LinkedHashMap<String, Integer>();
        public final Map<String, List<String>> samples = new LinkedHashMap<String, List<String>>();
        public final List<String> allNames = new ArrayList<String>();
    }

    public static Index index(Path clientJar) throws Exception {
        Index idx = new Index();
        ZipFile zip = new ZipFile(clientJar.toFile());
        try {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName();
                idx.totalEntries++;
                idx.allNames.add(name);
                String cat = categoryOf(name);
                Integer c = idx.categories.get(cat);
                idx.categories.put(cat, c == null ? 1 : c + 1);
                List<String> sample = idx.samples.get(cat);
                if (sample == null) { sample = new ArrayList<String>(); idx.samples.put(cat, sample); }
                if (sample.size() < 12) sample.add(name);
            }
        } finally { zip.close(); }
        Collections.sort(idx.allNames);
        return idx;
    }

    public static String categoryOf(String name) {
        if (name.startsWith("assets/minecraft/blockstates/")) return "blockstates";
        if (name.startsWith("assets/minecraft/models/block/")) return "models-block";
        if (name.startsWith("assets/minecraft/models/item/")) return "models-item";
        if (name.startsWith("assets/minecraft/textures/")) return "textures";
        if (name.startsWith("assets/minecraft/sounds/") || name.endsWith(".ogg")) return "sounds";
        if (name.contains("/recipe/") || name.contains("/recipes/")) return "recipes";
        if (name.contains("/loot_table/") || name.contains("/loot_tables/")) return "loot-tables";
        if (name.contains("/tags/")) return "tags";
        if (name.startsWith("data/")) return "data-other";
        if (name.startsWith("assets/")) return "assets-other";
        if (name.endsWith(".class")) return "classes";
        return "other";
    }

    public static String buildJson(String version, Index idx, boolean includeAllNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"project\": \"Gen5Craft\",\n  \"phase\": \"2.2\",\n");
        sb.append("  \"version\": \"").append(version).append("\",\n");
        sb.append("  \"totalEntries\": ").append(idx.totalEntries).append(",\n  \"categories\": {");
        boolean first = true;
        for (Map.Entry<String, Integer> e : idx.categories.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue());
        }
        sb.append("},\n  \"samples\": {");
        first = true;
        for (Map.Entry<String, List<String>> e : idx.samples.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(e.getKey()).append("\": [");
            for (int i = 0; i < e.getValue().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(esc(e.getValue().get(i))).append("\"");
            }
            sb.append("]");
        }
        sb.append("}");
        if (includeAllNames) {
            sb.append(",\n  \"allNames\": [");
            for (int i = 0; i < idx.allNames.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(esc(idx.allNames.get(i))).append("\"");
            }
            sb.append("]");
        }
        sb.append("\n}\n");
        return sb.toString();
    }

    public static Path writeFull(String version, Path clientJar, Path gameDir) throws Exception {
        Index idx = index(clientJar);
        Path out = gameDir.resolve("Datas/26.3-registry-index.json");
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Files.write(out, buildJson(version, idx, true).getBytes(StandardCharsets.UTF_8));
        return out;
    }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private RegistryExtractor() {}
}
