package com.gen5craft;

import com.gen5craft.core.BlockStateTable;
import com.gen5craft.core.ChunkColumn;
import com.gen5craft.core.ChunkDataDecoder;
import com.gen5craft.core.FeatureGate;
import com.gen5craft.core.Gen5Versions;
import com.gen5craft.core.DataSummary;
import com.gen5craft.core.GameDownloader;
import com.gen5craft.core.RegistryExtractor;
import com.gen5craft.core.VarInt;
import com.gen5craft.core.WorldModel;
import com.gen5craft.core.PerfProfile;
import com.gen5craft.core.Settings;
import com.gen5craft.net.LoginSession;
import com.gen5craft.net.PacketIO;
import com.gen5craft.render.CameraState;
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
        Settings strong = new Settings(); strong.quality = Settings.Quality.QUALITY; strong.fpsLimit = 120; strong.renderScalePercent = 100;
        check(PerfProfile.from(strong).targetFps == 120, "perf QUALITY keeps user FPS");
        check(PerfProfile.from(strong).effectiveScale(strong) == 100, "perf QUALITY full scale");
        Settings weak = new Settings(); weak.quality = Settings.Quality.PERFORMANCE; weak.fpsLimit = 120; weak.renderScalePercent = 100;
        check(PerfProfile.from(weak).staticScene, "perf PERFORMANCE is static");
        check(PerfProfile.from(weak).effectiveScale(weak) == 60, "perf PERFORMANCE caps scale at 60");
        check(com.gen5craft.launcher.LauncherMain.compareVersions("1.0.0", "0.9.9") > 0, "version compare 1.0.0 > 0.9.9");
        check(com.gen5craft.launcher.LauncherMain.compareVersions("0.6.0", "0.6.0") == 0, "version compare equal");
        check(com.gen5craft.launcher.LauncherMain.compareVersions("0.5.9", "0.6.0") < 0, "version compare older");
        CameraState cam = new CameraState();
        cam.rotate(Math.PI * 4.0);
        check(Math.abs(cam.yaw - 0.6) < 1e-9, "camera yaw wraps at 2*PI");
        cam.key('D'); check(cam.yaw > 0.6, "camera D rotates right");
        cam.key('W'); check(cam.zoom > 1.0, "camera W zooms in");
        for (int i = 0; i < 50; i++) cam.zoomBy(1.5);
        check(cam.zoom == 2.0, "camera zoom clamps at 2.0");
        for (int i = 0; i < 50; i++) cam.zoomBy(0.5);
        check(cam.zoom == 0.6, "camera zoom clamps at 0.6");
        java.awt.image.BufferedImage zoomed = SoftwareRenderer.renderToImage(160, 120, 0.6, 1.8);
        check(zoomed.getWidth() == 160, "zoomed render keeps buffer size");

        // ---- Phase 7: Gen-5 chunk-data decoding into the renderer ----
        check(ChunkDataDecoder.chunkPacketId(777) == 0x2E, "26.3 chunk packet id 0x2E");
        check(ChunkDataDecoder.chunkPacketId(766) == 0x27, "1.20.5 chunk packet id 0x27");
        check(ChunkDataDecoder.chunkPacketId(774) == 0x2C, "1.21.11 chunk packet id 0x2C");
        check(ChunkDataDecoder.chunkPacketId(775) == 0x2D, "26.1 chunk packet id 0x2D");
        check(ChunkDataDecoder.isChunkPacket(777, 0x2E) && !ChunkDataDecoder.isChunkPacket(777, 0x2D), "chunk packet detection by protocol");
        check(ChunkDataDecoder.keepAliveServerboundId(777) == 0x1C, "26.3 serverbound keep-alive id");
        check(ChunkDataDecoder.chunkBatchReceivedId(766) == 0x08, "1.20.5 serverbound chunk-batch id");
        check(Gen5Versions.byProtocol(777) != null && Gen5Versions.byProtocol(777).id.equals("26.3"), "protocol 777 -> 26.3");
        check(Gen5Versions.byProtocol(775) != null && Gen5Versions.byProtocol(775).id.equals("26.1"), "protocol 775 -> 26.1");
        BlockStateTable table263 = BlockStateTable.forProtocol(777);
        check(table263 != null && table263.version().equals("26.3"), "26.3 block-state table pinned");
        check(table263.isAir(0) && table263.isAir(18650), "26.3 air + cave_air are air");
        check(table263.category(9) == BlockStateTable.GRASS, "26.3 grass_block state 9 -> grass");
        check(table263.category(10) == BlockStateTable.DIRT, "26.3 dirt state 10 -> dirt");
        check(table263.category(89) == BlockStateTable.WATER, "26.3 water state 89 -> water");
        check(table263.category(1) == BlockStateTable.STONE, "26.3 stone state 1 -> stone");

        // Synthetic but wire-exact protocol-777 chunk: stone base section,
        // an indirect-palette surface section (grass west, water east, one
        // stone at the corner), then 22 all-air sections; 2048-byte sky light.
        ChunkColumn col = ChunkDataDecoder.decode(buildChunk777(), 777, -64, table263, true);
        check(col.chunkX == 3 && col.chunkZ == -2, "chunk coords decoded");
        check(col.sectionCount == 24, "24 sections decoded from data");
        check(col.nonAirBlocks == 4353L, "non-air blocks counted (4096+257)");
        check(col.fluidBlocks == 128L, "fluid blocks counted from 775+ field");
        check(col.heightmapTypes().contains(1), "WORLD_SURFACE heightmap present");
        check(col.blockEntityCount == 0, "no block entities");
        check(col.lightParsed && col.skyLightArrays == 1 && col.blockLightArrays == 0, "light data parsed (byte bitsets)");
        check(col.topStateAt(0, 0) == 9 && col.topYAt(0, 0) == -33, "top grass at (0,0), y=-33");
        check(col.topStateAt(15, 15) == 89, "top water at (15,15)");
        check(col.blockStateAt(0, -48, 0) == 1, "retained stone at section1 y=0 corner");
        check(col.blockStateAt(4, -33, 7) == 9, "retained grass in surface layer");
        SoftwareRenderer chunkRenderer = new SoftwareRenderer(col, table263);
        check(chunkRenderer.heightAt(0, 0) == 2, "renderer height from decoded top section");
        check(chunkRenderer.blockAt(0, 0) == 0, "renderer grass category from decoded state");
        check(chunkRenderer.blockAt(15, 15) == 4, "renderer water category from decoded state");
        java.awt.image.BufferedImage chunkFrame = SoftwareRenderer.renderToImage(160, 120, 0.6, 1.0, col, table263);
        java.util.Set<Integer> chunkColors = new java.util.HashSet<Integer>();
        for (int y = 0; y < chunkFrame.getHeight(); y += 6) for (int x = 0; x < chunkFrame.getWidth(); x += 6) chunkColors.add(chunkFrame.getRGB(x, y));
        check(chunkColors.size() > 5, "decoded chunk renders with varied colors (" + chunkColors.size() + ")");

        // Direct-palette container (bits=16, global state IDs inline).
        ChunkColumn direct = ChunkDataDecoder.decode(buildChunkDirect777(), 777, -64, table263, true);
        check(direct.sectionCount == 1 && direct.nonAirBlocks == 2L, "direct palette section decoded");
        check(direct.topStateAt(15, 15) == 9 && direct.topYAt(15, 15) == -49, "direct palette top grass");
        check(direct.blockStateAt(0, -64, 0) == 1, "direct palette stone at origin");

        // Indirect container with bits byte 1-3 is stored at width 4 (client rule).
        ChunkColumn narrow = ChunkDataDecoder.decode(buildChunkNarrow777(), 777, -64, table263, true);
        check(narrow.blockStateAt(5, -64, 0) == 10, "bits=2 block palette read at width 4");
        check(narrow.topStateAt(5, 0) == 10, "narrow palette top is dirt");

        // Legacy Gen-5 wire (protocol 766): NBT heightmaps, one block-count
        // short, data-array length prefixes, long-array light masks.
        ChunkColumn legacy = ChunkDataDecoder.decode(buildChunk766(), 766);
        check(legacy.sectionCount == 1 && legacy.nonAirBlocks == 4096L, "legacy 766 single-stone section decoded");
        check(legacy.topStateAt(0, 0) == 1 && legacy.topYAt(0, 0) == -49, "legacy 766 top stone");
        check(legacy.sectionFluidCounts[0] == -1, "legacy 766 has no fluid-count field");

        System.out.println("ALL TESTS PASSED");
    }

    // ---- Phase 7 test builders: wire-exact synthetic chunk packets ----
    private static void wInt(java.io.ByteArrayOutputStream o, int v) {
        o.write((v >>> 24) & 0xFF); o.write((v >>> 16) & 0xFF); o.write((v >>> 8) & 0xFF); o.write(v & 0xFF);
    }
    private static void wShort(java.io.ByteArrayOutputStream o, int v) {
        o.write((v >>> 8) & 0xFF); o.write(v & 0xFF);
    }
    private static void wLong(java.io.ByteArrayOutputStream o, long v) {
        wInt(o, (int) (v >>> 32)); wInt(o, (int) v);
    }
    private static void wVar(java.io.ByteArrayOutputStream o, int v) {
        byte[] b = VarInt.encode(v); o.write(b, 0, b.length);
    }
    private static void setEntry(long[] data, int index, int width, int v) {
        int perLong = 64 / width;
        data[index / perLong] |= (long) v << ((index % perLong) * width);
    }
    private static byte[] buildChunk777() throws Exception {
        java.io.ByteArrayOutputStream sec = new java.io.ByteArrayOutputStream();
        // Section 0: single-valued stone.
        wShort(sec, 4096); wShort(sec, 0);
        sec.write(0); wVar(sec, 1);          // block states: single stone (1)
        sec.write(0); wVar(sec, 0);          // biomes: single 0
        // Section 1: indirect palette [air, grass(9), water(89), stone(1)] at 4 bits.
        wShort(sec, 257); wShort(sec, 128);
        sec.write(4); wVar(sec, 4); wVar(sec, 0); wVar(sec, 9); wVar(sec, 89); wVar(sec, 1);
        long[] data = new long[256];
        setEntry(data, 0, 4, 3);             // stone at (0, y0, 0)
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            setEntry(data, 3840 + z * 16 + x, 4, x < 8 ? 1 : 2); // top layer: grass west, water east
        }
        for (long l : data) wLong(sec, l);
        sec.write(0); wVar(sec, 0);          // biomes: single 0
        // Sections 2..23: single-valued air.
        for (int i = 0; i < 22; i++) {
            wShort(sec, 0); wShort(sec, 0);
            sec.write(0); wVar(sec, 0);
            sec.write(0); wVar(sec, 0);
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        wInt(out, 3); wInt(out, -2);
        wVar(out, 1); wVar(out, 1); wVar(out, 37);           // one WORLD_SURFACE heightmap
        for (int i = 0; i < 37; i++) wLong(out, 0L);
        byte[] secBytes = sec.toByteArray();
        wVar(out, secBytes.length); out.write(secBytes);
        wVar(out, 0);                                        // block entities
        wVar(out, 1); out.write(1);                          // sky light mask (byte bitset)
        wVar(out, 0); wVar(out, 0); wVar(out, 0);            // other masks empty
        wVar(out, 1); wVar(out, 2048); out.write(new byte[2048]); // one sky light array
        wVar(out, 0);                                        // block light arrays
        return out.toByteArray();
    }
    private static byte[] buildChunkDirect777() throws Exception {
        java.io.ByteArrayOutputStream sec = new java.io.ByteArrayOutputStream();
        wShort(sec, 2); wShort(sec, 0);
        sec.write(16);                                       // direct palette, 16 bits
        long[] data = new long[1024];
        setEntry(data, 0, 16, 1);                            // stone at origin
        setEntry(data, 4095, 16, 9);                         // grass at (15, y15, 15)
        for (long l : data) wLong(sec, l);
        sec.write(0); wVar(sec, 0);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        wInt(out, 0); wInt(out, 0);
        wVar(out, 0);                                        // heightmaps
        byte[] secBytes = sec.toByteArray();
        wVar(out, secBytes.length); out.write(secBytes);
        wVar(out, 0);
        wVar(out, 0); wVar(out, 0); wVar(out, 0); wVar(out, 0);
        wVar(out, 0); wVar(out, 0);
        return out.toByteArray();
    }
    private static byte[] buildChunkNarrow777() throws Exception {
        java.io.ByteArrayOutputStream sec = new java.io.ByteArrayOutputStream();
        wShort(sec, 1); wShort(sec, 0);
        sec.write(2); wVar(sec, 2); wVar(sec, 0); wVar(sec, 10); // palette [air, dirt], stored at width 4
        long[] data = new long[256];
        setEntry(data, 5, 4, 1);                             // dirt at (5, y0, 0)
        for (long l : data) wLong(sec, l);
        sec.write(0); wVar(sec, 0);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        wInt(out, 0); wInt(out, 0);
        wVar(out, 0);
        byte[] secBytes = sec.toByteArray();
        wVar(out, secBytes.length); out.write(secBytes);
        wVar(out, 0);
        wVar(out, 0); wVar(out, 0); wVar(out, 0); wVar(out, 0);
        wVar(out, 0); wVar(out, 0);
        return out.toByteArray();
    }
    private static byte[] buildChunk766() throws Exception {
        java.io.ByteArrayOutputStream sec = new java.io.ByteArrayOutputStream();
        wShort(sec, 4096);                                   // block count only (pre-775)
        sec.write(0); wVar(sec, 1); wVar(sec, 0);            // single stone + legacy data length
        sec.write(0); wVar(sec, 0); wVar(sec, 0);            // biomes single + legacy data length
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        wInt(out, 1); wInt(out, 1);
        out.write(10);                                       // heightmaps: NBT compound
        out.write(12);                                       //   long-array entry
        byte[] hn = "MOTION_BLOCKING".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        wShort(out, hn.length); out.write(hn);
        wInt(out, 37); for (int i = 0; i < 37; i++) wLong(out, 0L);
        out.write(0);                                        // end compound
        byte[] secBytes = sec.toByteArray();
        wVar(out, secBytes.length); out.write(secBytes);
        wVar(out, 0);                                        // block entities
        wVar(out, 0); wVar(out, 0); wVar(out, 0); wVar(out, 0); // long-array light masks
        wVar(out, 0); wVar(out, 0);                          // light arrays
        return out.toByteArray();
    }
}
