package com.gen5craft.core;

/** Phase 7: decoder for the Gen-5 Clientbound Level Chunk With Light packet
 *  (minecraft:level_chunk_with_light) — the packet that carries real terrain
 *  from a server into the client.
 *
 *  Wire layout (protocol 766+, with the version differences noted):
 *    Int chunkX, Int chunkZ
 *    Heightmaps: protocol 770+ = Prefixed Array of {VarInt type, Long Array};
 *                protocol 766-769 = one network-NBT compound (skipped here)
 *    Data: VarInt size + chunk sections, bottom-to-top. Each section:
 *                Short non-air block count
 *                Short fluid count                 (protocol 775+ only)
 *                Paletted Container block states (4096 entries)
 *                Paletted Container biomes       (64 entries)
 *    Block Entities: Prefixed Array of {UByte packedXZ, Short y, VarInt type, NBT}
 *    Light Data: 4 BitSet masks + Prefixed Arrays of 2048-byte light arrays.
 *                Masks are Long Arrays up to protocol 776 and Byte BitSets
 *                (VarInt byte length + bytes) from protocol 777.
 *
 *  Paletted Container: one UByte bits-per-entry. 0 = single valued (one VarInt,
 *  no data). Block states: 1-8 = indirect (local palette of VarInts; storage
 *  width is max(4, bits)); above that = direct global state IDs. Biomes: 1-3 =
 *  indirect, above = direct. Packed entries never span longs (padding at the
 *  end of each long). Protocol 770+ no longer sends the data-array length; it
 *  is computed from bits and entry count. Older Gen-5 packets carry it.
 *
 *  Sections are self-delimiting, so the section count is read from the data
 *  itself instead of trusting a dimension height the login packet has not
 *  given us yet. Pure byte logic — fully unit-tested without any server. */
public final class ChunkDataDecoder {

    // ---- Play packet IDs per protocol (clientbound unless noted) ----
    public static int chunkPacketId(int protocol) {
        if (protocol < 766) return -1;
        if (protocol <= 767) return 0x27;
        if (protocol <= 769) return 0x28;
        if (protocol <= 772) return 0x27;
        if (protocol <= 774) return 0x2C;
        if (protocol <= 776) return 0x2D;
        return 0x2E;
    }
    public static int chunkBatchFinishedId(int protocol) {
        if (protocol < 766) return -1;
        return protocol <= 769 ? 0x0C : 0x0B;
    }
    /** Serverbound Chunk Batch Received packet ID. */
    public static int chunkBatchReceivedId(int protocol) {
        if (protocol < 766) return -1;
        if (protocol <= 767) return 0x08;
        if (protocol <= 770) return 0x09;
        if (protocol <= 774) return 0x0A;
        return 0x0B;
    }
    public static int keepAliveClientboundId(int protocol) {
        if (protocol < 766) return -1;
        if (protocol <= 767) return 0x26;
        if (protocol <= 769) return 0x27;
        if (protocol <= 772) return 0x26;
        if (protocol <= 774) return 0x2B;
        if (protocol <= 776) return 0x2C;
        return 0x2D;
    }
    /** Serverbound Keep Alive packet ID. */
    public static int keepAliveServerboundId(int protocol) {
        if (protocol < 766) return -1;
        if (protocol <= 767) return 0x18;
        if (protocol <= 770) return 0x1A;
        if (protocol <= 774) return 0x1B;
        return 0x1C;
    }
    public static boolean isChunkPacket(int protocol, int packetId) {
        return packetId >= 0 && packetId == chunkPacketId(protocol);
    }

    /** Decode with the overworld default minY (-64) and the protocol's pinned
     *  block-state table (fallback when none is pinned yet). Voxels are not
     *  retained (top surface + statistics only). */
    public static ChunkColumn decode(byte[] payload, int protocol) {
        BlockStateTable table = BlockStateTable.forProtocol(protocol);
        return decode(payload, protocol, -64, table != null ? table : BlockStateTable.FALLBACK, false);
    }

    public static ChunkColumn decode(byte[] payload, int protocol, int minY, BlockStateTable table, boolean retainBlocks) {
        if (payload == null) throw new IllegalArgumentException("null chunk payload");
        if (protocol < 766) throw new IllegalArgumentException("protocol " + protocol + " is below Generation 5 (766+)");
        if (table == null) table = BlockStateTable.FALLBACK;
        Reader r = new Reader(payload);
        int chunkX = r.readInt();
        int chunkZ = r.readInt();
        ChunkColumn chunk = new ChunkColumn(chunkX, chunkZ, minY);
        chunk.blockStateTableVersion = table.version();

        // Heightmaps
        if (protocol >= 770) {
            int count = r.readVarInt();
            if (count < 0 || count > 16) throw new IllegalArgumentException("bad heightmap count " + count);
            for (int i = 0; i < count; i++) {
                int type = r.readVarInt();
                int longs = r.readVarInt();
                if (longs < 0 || longs > 4096) throw new IllegalArgumentException("bad heightmap size " + longs);
                r.skip(longs * 8L);
                chunk.heightmapTypes.add(type);
            }
        } else {
            skipNbt(r); // single network-NBT compound (or 0x00 when absent)
        }

        // Chunk sections (self-delimiting inside the size-prefixed data array)
        int dataSize = r.readVarInt();
        if (dataSize < 0 || dataSize > r.remaining()) throw new IllegalArgumentException("bad chunk data size " + dataSize);
        Reader s = new Reader(r.readBytes(dataSize));
        java.util.ArrayList<int[]> sections = new java.util.ArrayList<int[]>();
        java.util.ArrayList<int[]> counts = new java.util.ArrayList<int[]>();
        while (s.remaining() > 0) {
            int blockCount = s.readUnsignedShort();
            int fluidCount = protocol >= 775 ? s.readUnsignedShort() : -1;
            int[] states = readPalettedContainer(s, 4096, true, protocol);
            readPalettedContainer(s, 64, false, protocol); // biomes: parsed and validated, not kept yet
            sections.add(states);
            counts.add(new int[]{blockCount, fluidCount});
        }
        chunk.initSections(sections.size());
        if (retainBlocks) chunk.retainBlocks();
        for (int sec = 0; sec < sections.size(); sec++) {
            int[] states = sections.get(sec);
            chunk.sectionBlockCounts[sec] = counts.get(sec)[0];
            chunk.sectionFluidCounts[sec] = counts.get(sec)[1];
            chunk.nonAirBlocks += counts.get(sec)[0];
            if (counts.get(sec)[1] > 0) chunk.fluidBlocks += counts.get(sec)[1];
            int baseY = minY + sec * 16;
            for (int i = 0; i < 4096; i++) {
                int state = states[i];
                chunk.setBlock(sec, i, state);
                if (!table.isAir(state)) {
                    int x = i & 15, z = (i >> 4) & 15, y = baseY + ((i >> 8) & 15);
                    chunk.offerTop(x, z, y, state);
                }
            }
        }

        // Block entities (counted; NBT payloads skipped with a real NBT walker)
        try {
            int beCount = r.readVarInt();
            if (beCount < 0 || beCount > 65536) throw new IllegalArgumentException("bad block entity count " + beCount);
            chunk.blockEntityCount = beCount;
            for (int i = 0; i < beCount; i++) {
                r.skip(1);          // packed XZ
                r.skip(2);          // Y (short)
                r.readVarInt();     // block entity type
                if (r.peek() != 0) skipNbt(r); else r.skip(1); // optional NBT tag
            }
        } catch (RuntimeException e) {
            chunk.blockEntityCount = 0; // terrain is already decoded; never fail the chunk on this
        }

        // Light data (trailing; parsed for statistics, tolerated if malformed)
        try {
            for (int i = 0; i < 4; i++) {
                if (protocol >= 777) { int n = r.readVarInt(); r.skip(n); }
                else { int n = r.readVarInt(); r.skip(n * 8L); }
            }
            chunk.skyLightArrays = readLightArrays(r);
            chunk.blockLightArrays = readLightArrays(r);
            chunk.lightParsed = true;
        } catch (RuntimeException e) {
            chunk.lightParsed = false;
        }
        return chunk;
    }

    private static int readLightArrays(Reader r) {
        int count = r.readVarInt();
        if (count < 0 || count > 4096) throw new IllegalArgumentException("bad light array count " + count);
        for (int i = 0; i < count; i++) {
            int len = r.readVarInt();
            if (len < 0) throw new IllegalArgumentException("bad light array length " + len);
            r.skip(len);
        }
        return count;
    }

    /** Reads one Paletted Container and returns the global IDs of all entries. */
    static int[] readPalettedContainer(Reader r, int entries, boolean blockStates, int protocol) {
        int bits = r.readUByte();
        int[] out = new int[entries];
        if (bits == 0) {
            int single = r.readVarInt();
            if (protocol < 770) r.readVarInt(); // legacy empty data-array length (always 0)
            for (int i = 0; i < entries; i++) out[i] = single;
            return out;
        }
        boolean indirect;
        int width;
        if (blockStates) {
            indirect = bits <= 8;
            width = indirect ? Math.max(4, bits) : bits;
        } else {
            indirect = bits <= 3;
            width = bits;
        }
        if (width < 1 || width > 32) throw new IllegalArgumentException("bad bits per entry " + bits);
        int[] palette = null;
        if (indirect) {
            int n = r.readVarInt();
            if (n <= 0 || n > 65536) throw new IllegalArgumentException("bad palette size " + n);
            palette = new int[n];
            for (int i = 0; i < n; i++) palette[i] = r.readVarInt();
        }
        long[] data;
        if (protocol >= 770) {
            int perLong = 64 / width;
            int nLongs = (entries + perLong - 1) / perLong;
            data = new long[nLongs];
            for (int i = 0; i < nLongs; i++) data[i] = r.readLong();
        } else {
            int nLongs = r.readVarInt();
            if (nLongs < 0 || nLongs > 65536) throw new IllegalArgumentException("bad data array length " + nLongs);
            data = new long[nLongs];
            for (int i = 0; i < nLongs; i++) data[i] = r.readLong();
        }
        int perLong = 64 / width;
        long mask = (1L << width) - 1L;
        for (int i = 0; i < entries; i++) {
            int li = i / perLong;
            if (li >= data.length) throw new IllegalArgumentException("paletted data too short");
            int v = (int) ((data[li] >>> ((i % perLong) * width)) & mask);
            if (indirect) {
                if (v >= palette.length) throw new IllegalArgumentException("palette index " + v + " out of " + palette.length);
                out[i] = palette[v];
            } else {
                out[i] = v;
            }
        }
        return out;
    }

    // ---- Network NBT walker (anonymous root, as on the wire since 1.20.2) ----
    static void skipNbt(Reader r) {
        int type = r.readUByte();
        if (type == 0) return;
        skipNbtPayload(r, type);
    }
    private static void skipNbtPayload(Reader r, int type) {
        switch (type) {
            case 1: r.skip(1); break;
            case 2: r.skip(2); break;
            case 3: case 5: r.skip(4); break;
            case 4: case 6: r.skip(8); break;
            case 7: { int n = r.readInt(); if (n < 0) throw new IllegalArgumentException("bad NBT byte array"); r.skip(n); break; }
            case 8: { int n = r.readUnsignedShort(); r.skip(n); break; }
            case 9: {
                int elem = r.readUByte(); int n = r.readInt();
                if (n < 0 || n > 1_000_000) throw new IllegalArgumentException("bad NBT list");
                for (int i = 0; i < n; i++) skipNbtPayload(r, elem);
                break;
            }
            case 10: {
                while (true) {
                    int t = r.readUByte();
                    if (t == 0) break;
                    int n = r.readUnsignedShort(); r.skip(n); // entry name
                    skipNbtPayload(r, t);
                }
                break;
            }
            case 11: { int n = r.readInt(); if (n < 0) throw new IllegalArgumentException("bad NBT int array"); r.skip(n * 4L); break; }
            case 12: { int n = r.readInt(); if (n < 0) throw new IllegalArgumentException("bad NBT long array"); r.skip(n * 8L); break; }
            default: throw new IllegalArgumentException("unknown NBT tag " + type);
        }
    }

    /** Big-endian byte reader over a packet payload. */
    static final class Reader {
        private final byte[] data; private int pos;
        Reader(byte[] data) { this.data = data; }
        int remaining() { return data.length - pos; }
        int peek() { if (pos >= data.length) throw new IllegalArgumentException("overrun"); return data[pos] & 0xFF; }
        int readUByte() { if (pos >= data.length) throw new IllegalArgumentException("overrun"); return data[pos++] & 0xFF; }
        int readUnsignedShort() { return (readUByte() << 8) | readUByte(); }
        int readInt() { return (readUByte() << 24) | (readUByte() << 16) | (readUByte() << 8) | readUByte(); }
        long readLong() { return ((long) readInt() << 32) | (readInt() & 0xFFFFFFFFL); }
        double readDouble() { return Double.longBitsToDouble(readLong()); }
        int readVarInt() {
            int[] p = {pos};
            int v = VarInt.decode(data, p);
            pos = p[0];
            return v;
        }
        byte[] readBytes(int n) {
            if (n < 0 || n > remaining()) throw new IllegalArgumentException("overrun");
            byte[] b = new byte[n];
            System.arraycopy(data, pos, b, 0, n);
            pos += n;
            return b;
        }
        void skip(long n) {
            if (n < 0 || n > remaining()) throw new IllegalArgumentException("overrun");
            pos += (int) n;
        }
    }
    private ChunkDataDecoder() {}
}
