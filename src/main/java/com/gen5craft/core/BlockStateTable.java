package com.gen5craft.core;

/** Phase 7: maps global block-state IDs (as sent in chunk data) to renderer
 *  categories, per game version. Global state IDs are assigned by the block
 *  registry of one exact version, so a table is only valid for its version.
 *
 *  Categories: 0 grass, 1 dirt, 2 stone, 3 sand, 4 water, 5 leaves, 6 wood,
 *  7 snow, 8 lava. AIR is -1. */
public interface BlockStateTable {
    int AIR = -1;
    int GRASS = 0, DIRT = 1, STONE = 2, SAND = 3, WATER = 4, LEAVES = 5, WOOD = 6, SNOW = 7, LAVA = 8;

    boolean isAir(int stateId);
    int category(int stateId);
    String version();

    /** Phase 8: solidity for player physics. Fluids and air are not solid;
     *  everything else is treated as a full cube (slab/stair shapes are a
     *  later refinement, documented as such). */
    default boolean isSolid(int stateId) {
        int c = category(stateId);
        return c != AIR && c != WATER && c != LAVA;
    }

    /** Table for the given protocol, or null when no pinned table exists yet.
     *  Callers then fall back to air==0 and elevation-based coloring, while the
     *  decoded state IDs and heights stay exact. */
    static BlockStateTable forProtocol(int protocol) {
        if (protocol == 777) return V263.INSTANCE;
        return null;
    }

    /** Fallback used for Gen-5 versions without a pinned state table yet. */
    BlockStateTable FALLBACK = new BlockStateTable() {
        public boolean isAir(int stateId) { return stateId == 0; }
        public int category(int stateId) { return STONE; }
        public boolean isSolid(int stateId) { return stateId != 0; }
        public String version() { return "fallback (air=0 only)"; }
    };

    final class V263 implements BlockStateTable {
        static final V263 INSTANCE = new V263();
        public boolean isAir(int stateId) { return category(stateId) == AIR; }
        public int category(int stateId) {
            int[] s = BlockStateTable263.START, e = BlockStateTable263.END;
            int lo = 0, hi = s.length - 1, best = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (s[mid] <= stateId) { best = mid; lo = mid + 1; } else { hi = mid - 1; }
            }
            if (best < 0 || stateId > e[best]) return STONE;
            return BlockStateTable263.CAT[best];
        }
        public String version() { return "26.3"; }
    }
}
