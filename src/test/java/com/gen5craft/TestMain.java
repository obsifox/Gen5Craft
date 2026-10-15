package com.gen5craft;

import com.gen5craft.core.FeatureGate;
import com.gen5craft.core.Gen5Versions;
import com.gen5craft.core.DataSummary;
import com.gen5craft.core.GameDownloader;
import com.gen5craft.core.RegistryExtractor;
import com.gen5craft.core.VarInt;
import com.gen5craft.core.WorldModel;
import com.gen5craft.net.LoginSession;
import com.gen5craft.net.PacketIO;
import com.gen5craft.render.SoftwareRenderer;

/** Dependency-free test harness (no JUnit download needed on CI). */
public final class TestMain {
    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
        System.out.println("PASS: " + msg);
    }
    public static void main(String[] args) throws Exception {
        check(Gen5Versions.isGen5("1.20.5"), "1.20.5 is Gen5 (first)");
        check(Gen5Versions.isGen5("26.3"), "26.3 is Gen5 (latest)");
        check(!Gen5Versions.isGen5("1.20.4"), "1.20.4 is NOT Gen5 (previous generation)");
        check(!Gen5Versions.isGen5("1.12.2"), "1.12.2 is NOT Gen5");
        check(Gen5Versions.latest().equals("26.3"), "latest is 26.3");
        FeatureGate.Profile latest = FeatureGate.forServer("26.3");
        check(latest.allowed && latest.disabledFeatures.isEmpty(), "26.3 server disables nothing");
        FeatureGate.Profile older = FeatureGate.forServer("1.20.5");
        check(older.allowed && !older.disabledFeatures.isEmpty(), "1.20.5 server disables newer content");
        check(older.disabledFeatures.contains("mace"), "mace (1.21) disabled on 1.20.5 server");
        check(!FeatureGate.forServer("1.20.4").allowed, "1.20.4 server rejected as other generation");
        check(FeatureGate.forLocalWorld().disabledFeatures.isEmpty(), "local world disables nothing");
        int[] values = {0, 1, 127, 128, 766, 774, 25565, Integer.MAX_VALUE};
        for (int v : values) {
            int[] pos = {0};
            check(VarInt.decode(VarInt.encode(v), pos) == v, "VarInt roundtrip " + v);
        }
        check(Gen5Versions.byProtocol(766) != null && Gen5Versions.byProtocol(766).id.equals("1.20.5"), "protocol 766 -> 1.20.5");
        check(Gen5Versions.byProtocol(774) != null && Gen5Versions.byProtocol(774).id.equals("1.21.11"), "protocol 774 -> 1.21.11");
        check(Gen5Versions.byProtocol(765) == null, "protocol 765 (1.20.4) is previous generation");
        GameDownloader.Plan plan = new GameDownloader.Plan();
        plan.versionId = "26.3"; plan.clientSha1 = "e877b6a07acd633fb3bb475002175cec036e7b87";
        plan.clientSize = 41483720L; plan.assetIndexId = "34";
        plan.libraries.add(new String[]{"https://example.invalid/lib.jar", "abc", "lib.jar"});
        String summary = DataSummary.buildJson(plan);
        check(summary.contains("\"version\": \"26.3\""), "data summary carries 26.3");
        check(summary.contains("\"librariesCount\": 1"), "data summary counts libraries");
        String fakeManifest = "{\"files\": [{\"name\": \"Gen5Craft.jar\", \"sha256\": \"deadbeef\"}]}";
        check("deadbeef".equals(com.gen5craft.launcher.LauncherMain.extractSha(fakeManifest, "Gen5Craft.jar")), "launcher reads sha256 from manifest");
        check(RegistryExtractor.categoryOf("assets/minecraft/blockstates/stone.json").equals("blockstates"), "category blockstates");
        check(RegistryExtractor.categoryOf("data/minecraft/recipe/x.json").equals("recipes"), "category recipes");
        RegistryExtractor.Index idx = new RegistryExtractor.Index();
        idx.totalEntries = 2; idx.categories.put("blockstates", 2); idx.allNames.add("a"); idx.allNames.add("b");
        check(RegistryExtractor.buildJson("26.3", idx, false).contains("\"totalEntries\": 2"), "registry index json");
        java.nio.file.Path tmpSaves = java.nio.file.Files.createTempDirectory("gen5-test-saves");
        WorldModel w = WorldModel.create(tmpSaves, "My World");
        check(w.dataVersion.equals("26.3"), "world pinned to latest 26.3 data");
        check(WorldModel.list(tmpSaves).size() == 1, "world listed after create");
        check(WorldModel.fromJson(w.toJson(), "x").name.equals(w.name), "world metadata roundtrip");
        SoftwareRenderer renderer = new SoftwareRenderer();
        boolean heightsOk = true;
        for (int x = 0; x < SoftwareRenderer.SIZE; x++) for (int z = 0; z < SoftwareRenderer.SIZE; z++) {
            int h = renderer.heightAt(x, z); if (h < 1 || h > 9) heightsOk = false;
        }
        check(heightsOk, "voxel heights within 1..9");
        check(renderer.waterColumns() > 0, "scene contains water columns");
        java.awt.image.BufferedImage frame = SoftwareRenderer.renderToImage(160, 120, 0.6);
        java.util.Set<Integer> colors = new java.util.HashSet<Integer>();
        for (int y = 0; y < frame.getHeight(); y += 6) for (int x = 0; x < frame.getWidth(); x += 6) colors.add(frame.getRGB(x, y));
        check(colors.size() > 5, "rendered frame has varied colors (" + colors.size() + ")");
        java.awt.image.BufferedImage frame2 = SoftwareRenderer.renderToImage(160, 120, 0.6);
        check(frame.getRGB(80, 60) == frame2.getRGB(80, 60), "rendering is deterministic");
        // Phase 4: packet framing (plain + compressed) and offline login builders
        byte[] framed = PacketIO.frame(0x2B, new byte[]{1, 2, 3}, -1);
        PacketIO.Packet back = PacketIO.readPacket(new java.io.ByteArrayInputStream(framed), -1);
        check(back.id == 0x2B && back.payload.length == 3, "packet framing roundtrip (uncompressed)");
        byte[] big = new byte[500]; for (int i = 0; i < big.length; i++) big[i] = (byte) (i % 7);
        byte[] framedC = PacketIO.frame(0x10, big, 16);
        PacketIO.Packet backC = PacketIO.readPacket(new java.io.ByteArrayInputStream(framedC), 16);
        check(backC.id == 0x10 && backC.payload.length == 500, "packet framing roundtrip (zlib compressed)");
        check(LoginSession.offlineUuid("Gen5Player").equals(LoginSession.offlineUuid("Gen5Player")), "offline UUID deterministic");
        check(LoginSession.offlineUuid("Gen5Player").version() == 3, "offline UUID is version 3");
        check(!LoginSession.offlineUuid("Alice").equals(LoginSession.offlineUuid("Bob")), "offline UUID differs per name");
        check(LoginSession.buildHandshake(766, "localhost", 25565).length > 8, "handshake packet builds");
        check(LoginSession.buildLoginStart("Gen5Player").length == 1 + 10 + 16, "login start = name + UUID");
        check(LoginSession.buildKnownPacks("1.21.5").length > 10, "known packs packet builds");
        System.out.println("ALL TESTS PASSED");
    }
}
