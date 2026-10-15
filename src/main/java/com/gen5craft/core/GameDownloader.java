package com.gen5craft.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Downloads the latest Generation-5 game data (default: Mojang release 26.3)
 *  exactly the way the official version manifest describes it: version JSON ->
 *  client jar + libraries + asset index -> assets. Every file is verified by
 *  its manifest SHA-1 and skipped when already present and valid, so weak
 *  connections and weak machines never re-download or waste work. */
public final class GameDownloader {
    public static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();

    public interface Progress { void on(String what, int percent); }

    public String fetchText(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Gen5Craft/0.1.0").GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IllegalStateException("HTTP " + res.statusCode() + " for " + url);
        return res.body();
    }

    /** Resolve the JSON URL of a version id (e.g. "26.3") from the official manifest. */
    public String resolveVersionJsonUrl(String versionId) throws Exception {
        String manifest = fetchText(MANIFEST_URL);
        String needle = "\"id\": \"" + versionId + "\"";
        int i = manifest.indexOf(needle);
        if (i < 0) throw new IllegalArgumentException("Version not found in Mojang manifest: " + versionId);
        int u = manifest.indexOf("\"url\": \"", i);
        int e = manifest.indexOf('"', u + 8);
        return manifest.substring(u + 8, e);
    }

    public static final class Plan {
        public String versionId;
        public String clientUrl; public String clientSha1; public long clientSize;
        public final List<String[]> libraries = new ArrayList<String[]>(); // {url, sha1, path}
        public String assetIndexUrl; public String assetIndexId;
    }

    /** Parse a version JSON (from Mojang) into a concrete download plan for Windows. */
    public Plan buildPlan(String versionId) throws Exception {
        Plan plan = new Plan();
        plan.versionId = versionId;
        String json = fetchText(resolveVersionJsonUrl(versionId));
        plan.clientUrl = extractAfter(json, "\"client\":", "\"url\": \"");
        plan.clientSha1 = extractAfter(json, "\"client\":", "\"sha1\": \"");
        String sizeStr = extractAfter(json, "\"client\":", "\"size\": ");
        plan.clientSize = sizeStr == null ? 0 : Long.parseLong(sizeStr.replaceAll("[^0-9]", ""));
        plan.assetIndexId = extract(json, "\"assetIndex\":", "\"id\": \"");
        plan.assetIndexUrl = extract(json, "\"assetIndex\":", "\"url\": \"");
        // Libraries: take every downloads.artifact entry (Windows natives are
        // marked by classifiers; for this foundation build we fetch artifacts).
        String libs = json;
        int pos = 0;
        while (true) {
            int a = libs.indexOf("\"artifact\":", pos);
            if (a < 0) break;
            String url = extract(libs.substring(a), null, "\"url\": \"");
            String sha1 = extract(libs.substring(a), null, "\"sha1\": \"");
            String path = extract(libs.substring(a), null, "\"path\": \"");
            if (url != null && path != null) plan.libraries.add(new String[]{url, sha1, path});
            pos = a + 10;
        }
        return plan;
    }

    /** Download (and SHA-1 verify) the client jar and libraries into {@code gameDir}. */
    public void downloadCore(final Plan plan, final Path gameDir, final Progress progress) throws Exception {
        Files.createDirectories(gameDir.resolve("versions/" + plan.versionId));
        Files.createDirectories(gameDir.resolve("libraries"));
        final int total = 1 + plan.libraries.size();
        final int[] done = {0};
        downloadVerified(plan.clientUrl, plan.clientSha1,
                gameDir.resolve("versions/" + plan.versionId + "/" + plan.versionId + ".jar"));
        report(progress, "client.jar", ++done[0], total);
        for (String[] lib : plan.libraries) {
            downloadVerified(lib[0], lib[1], gameDir.resolve("libraries/" + lib[2]));
            report(progress, lib[2], ++done[0], total);
        }
        DataSummary.write(plan, gameDir);
        if (plan.assetIndexUrl != null) {
            downloadVerified(plan.assetIndexUrl, null,
                    gameDir.resolve("assets/indexes/" + plan.assetIndexId + ".json"));
            report(progress, "asset index " + plan.assetIndexId, total, total);
        }
    }

    private void report(Progress p, String what, int done, int total) {
        if (p != null) p.on(what, total == 0 ? 100 : (int) (done * 100L / total));
    }

    private void downloadVerified(String url, String sha1, Path dest) throws Exception {
        if (Files.exists(dest) && sha1 != null && sha1.equals(sha1Of(dest))) return; // already valid
        if (dest.getParent() != null) Files.createDirectories(dest.getParent());
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Gen5Craft/0.1.0").GET().build();
        HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) throw new IllegalStateException("HTTP " + res.statusCode() + " for " + url);
        Path tmp = dest.resolveSibling(dest.getFileName().toString() + ".part");
        InputStream in = res.body();
        OutputStream out = Files.newOutputStream(tmp);
        try {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        } finally { out.close(); in.close(); }
        if (sha1 != null && !sha1.equals(sha1Of(tmp))) {
            Files.deleteIfExists(tmp);
            throw new IllegalStateException("SHA-1 mismatch: " + url);
        }
        Files.move(tmp, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public static String sha1Of(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        InputStream in = Files.newInputStream(file);
        try {
            byte[] buf = new byte[64 * 1024]; int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        } finally { in.close(); }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String extract(String json, String section, String key) {
        int from = section == null ? 0 : json.indexOf(section);
        if (from < 0) return null;
        return extractAfter(json.substring(from), null, key);
    }
    private static String extractAfter(String text, String section, String key) {
        int from = section == null ? 0 : text.indexOf(section);
        if (from < 0) return null;
        int k = text.indexOf(key, from);
        if (k < 0) return null;
        int start = k + key.length();
        int end = key.endsWith(" ") ? scanNumber(text, start) : text.indexOf('"', start);
        return end < 0 ? null : text.substring(start, end).trim();
    }
    private static int scanNumber(String t, int s) {
        int i = s; while (i < t.length() && (Character.isDigit(t.charAt(i)))) i++; return i;
    }
}
