package com.gen5craft.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/** Phase 2.2 (first slice): after the official Gen-5 data is downloaded,
 *  index what was fetched (client jar, libraries, asset index) into a small
 *  machine-readable summary. This is the foundation for the full registry
 *  extraction that follows; it never invents registry contents. */
public final class DataSummary {
    public static String buildJson(GameDownloader.Plan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"project\": \"Gen5Craft\",\n");
        sb.append("  \"generation\": 5,\n");
        sb.append("  \"version\": \"").append(plan.versionId).append("\",\n");
        sb.append("  \"generatedUtc\": \"").append(Instant.now().toString()).append("\",\n");
        sb.append("  \"client\": {\"sha1\": \"").append(plan.clientSha1).append("\", \"bytes\": ").append(plan.clientSize).append("},\n");
        sb.append("  \"librariesCount\": ").append(plan.libraries.size()).append(",\n");
        sb.append("  \"assetIndex\": \"").append(plan.assetIndexId).append("\",\n");
        sb.append("  \"note\": \"Manifest-level index of downloaded official data; registry extraction is the next slice.\"\n");
        sb.append("}\n");
        return sb.toString();
    }
    public static Path write(GameDownloader.Plan plan, Path gameDir) throws Exception {
        Path out = gameDir.resolve("Datas/gen5-data-summary.json");
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Files.write(out, buildJson(plan).getBytes(StandardCharsets.UTF_8));
        return out;
    }
    private DataSummary() {}
}
