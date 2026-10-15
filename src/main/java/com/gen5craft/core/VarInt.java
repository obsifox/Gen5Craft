package com.gen5craft.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Minecraft VarInt codec — the framing primitive of every Gen-5 protocol packet. */
public final class VarInt {
    public static byte[] encode(int value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(5);
        int v = value;
        do {
            byte b = (byte) (v & 0x7F);
            v >>>= 7;
            if (v != 0) b |= (byte) 0x80;
            out.write(b);
        } while (v != 0);
        return out.toByteArray();
    }
    public static int decode(byte[] data, int[] pos) {
        int result = 0, shift = 0, i = pos[0];
        while (true) {
            if (i >= data.length || shift > 28) throw new IllegalArgumentException("Bad VarInt");
            byte b = data[i++];
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        pos[0] = i;
        return result;
    }
    public static int read(InputStream in) throws IOException {
        int result = 0, shift = 0;
        while (true) {
            int b = in.read();
            if (b == -1 || shift > 28) throw new IOException("Bad VarInt from stream");
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
    }
    private VarInt() {}
}
