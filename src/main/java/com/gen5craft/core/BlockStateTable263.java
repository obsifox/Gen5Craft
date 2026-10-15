package com.gen5craft.core;

/** Block-state classification for Minecraft 26.3 (protocol 777), generated
 *  from the block registry extracted out of Mojang's official 26.3 server jar
 *  (data: mj41/mc26-data branch mc-26.3, extracted 2026-10-12 from the piston-data
 *  server jar, SHA-1 33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c). Each entry is a
 *  contiguous global block-state ID range of one block, mapped to a renderer
 *  category. Ranges not listed are category STONE (the common case). State IDs
 *  are version-specific: this table is only valid for 26.3. */
final class BlockStateTable263 {
    static final int MAX_STATE_ID = 35722;
    static final int[] START = {
        0, 8, 10, 15, 89, 105, 121, 139,
        177, 210, 267, 677, 2363, 2368, 6875, 8589,
        8616, 8668, 9628, 10068, 10652, 14761, 14770, 15094,
        15328, 15330, 16796, 16822, 18403, 18632, 18649, 24302,
        24319, 24389, 30519, 33663, 33712, 33745, 33771,
    };
    static final int[] END = {
        0, 9, 13, 28, 104, 120, 131, 168,
        206, 266, 658, 679, 2363, 2372, 6882, 8598,
        8616, 8669, 9691, 10091, 10653, 14761, 14771, 15096,
        15328, 15330, 16796, 16825, 18418, 18632, 18650, 24313,
        24330, 24390, 30519, 33663, 33712, 33752, 33772,
    };
    static final byte[] CAT = {
        -1, 0, 1, 6, 4, 8, 3, 6, 6, 6, 5, 3,
        0, 0, 1, 7, 1, 1, 6, 6, 0, 7, 0, 3,
        3, 3, 1, 7, 3, 7, -1, 6, 6, 6, 7, 0,
        0, 6, 1,
    };
    private BlockStateTable263() {}
}
