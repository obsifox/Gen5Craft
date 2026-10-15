package com.gen5craft.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Phase 7: one decoded chunk column (16x16 blocks, full world height).
 *
 *  Produced by ChunkDataDecoder from a real Gen-5 Clientbound Level Chunk
 *  With Light packet. The renderer only needs the top surface, so by default
 *  only per-column top block state / top Y plus section statistics are kept;
 *  callers may retain the full voxel array (sections * 4096 state IDs). */
public final class ChunkColumn {
    public final int chunkX;
    public final int chunkZ;
    public final int minY;              // world Y of section 0 bottom (overworld: -64)
    public int sectionCount;
    public final List<Integer> heightmapTypes = new ArrayList<Integer>();
    public int[] sectionBlockCounts = new int[0];  // non-air blocks per section (as sent)
    public int[] sectionFluidCounts = new int[0];  // fluids per section (protocol 775+; else -1)
    public long nonAirBlocks;           // sum of section block counts
    public long fluidBlocks;
    public int blockEntityCount;
    public int skyLightArrays;          // 2048-byte arrays decoded from Light Data
    public int blockLightArrays;
    public boolean lightParsed;
    public String blockStateTableVersion = "";

    private final int[] topState = new int[256];   // global state ID, -1 when column empty
    private final int[] topY = new int[256];       // absolute world Y of top non-air block
    private int[] blocks;                          // retained voxels, section-major; null unless retained

    ChunkColumn(int chunkX, int chunkZ, int minY) {
        this.chunkX = chunkX; this.chunkZ = chunkZ; this.minY = minY;
        for (int i = 0; i < 256; i++) { topState[i] = -1; topY[i] = Integer.MIN_VALUE; }
    }

    void initSections(int n) {
        sectionCount = n;
        sectionBlockCounts = new int[n];
        sectionFluidCounts = new int[n];
        for (int i = 0; i < n; i++) sectionFluidCounts[i] = -1;
    }
    void retainBlocks() { blocks = new int[Math.max(1, sectionCount) * 4096]; }
    void setBlock(int section, int index, int stateId) {
        if (blocks != null) blocks[section * 4096 + index] = stateId;
    }
    void offerTop(int x, int z, int y, int stateId) {
        int i = (z << 4) | x;
        if (y > topY[i]) { topY[i] = y; topState[i] = stateId; }
    }

    /** Global block-state ID at a local position, or -1 when not retained / out of range. */
    public int blockStateAt(int x, int y, int z) {
        if (blocks == null) return -1;
        int section = (y - minY) >> 4;
        if (x < 0 || x > 15 || z < 0 || z > 15 || section < 0 || section >= sectionCount) return -1;
        int index = ((y - minY) & 15) * 256 + z * 16 + x;
        return blocks[section * 4096 + index];
    }
    public boolean hasRetainedBlocks() { return blocks != null; }
    /** Top (highest) non-air global state ID of a column, -1 when empty. */
    public int topStateAt(int x, int z) { return topState[(z << 4) | x]; }
    /** Absolute world Y of the top non-air block of a column, Integer.MIN_VALUE when empty. */
    public int topYAt(int x, int z) { return topY[(z << 4) | x]; }
    /** Section index (0-based from minY) containing the top block, -1 when empty. */
    public int topSectionAt(int x, int z) {
        int y = topYAt(x, z);
        return y == Integer.MIN_VALUE ? -1 : (y - minY) >> 4;
    }
    /** Number of columns whose top block state equals the given global state ID. */
    public int columnsWithTopState(int stateId) {
        int c = 0;
        for (int s : topState) if (s == stateId) c++;
        return c;
    }
    public List<Integer> heightmapTypes() { return Collections.unmodifiableList(heightmapTypes); }

    public String summary() {
        int filled = 0, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int i = 0; i < 256; i++) if (topState[i] >= 0) {
            filled++;
            if (topY[i] < lo) lo = topY[i];
            if (topY[i] > hi) hi = topY[i];
        }
        return "chunk (" + chunkX + "," + chunkZ + ") sections=" + sectionCount
                + " nonAir=" + nonAirBlocks + " fluids=" + fluidBlocks
                + " columns=" + filled + "/256"
                + (filled > 0 ? " topY " + lo + ".." + hi : "")
                + " blockEntities=" + blockEntityCount
                + " light=" + (lightParsed ? (skyLightArrays + " sky/" + blockLightArrays + " block") : "unparsed")
                + " statesTable=" + blockStateTableVersion;
    }
}
