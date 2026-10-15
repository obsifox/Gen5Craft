package com.gen5craft.net;

import com.gen5craft.core.VarInt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/** Length-prefixed Minecraft packet framing, with the optional zlib
 *  compression negotiated during login (Set Compression).
 *  Wire format (uncompressed): VarInt length | VarInt packetId | payload.
 *  Wire format (compressed):   VarInt length | VarInt dataLength | data,
 *  where dataLength is the uncompressed size, or 0 when sent uncompressed.
 *  Pure byte logic — fully unit-tested without any server. */
public final class PacketIO {
    public static final class Packet {
        public final int id;
        public final byte[] payload;
        public Packet(int id, byte[] payload) { this.id = id; this.payload = payload; }
    }

    public static byte[] frame(int packetId, byte[] payload, int compressionThreshold) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(VarInt.encode(packetId));
        if (payload != null) data.write(payload);
        byte[] raw = data.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (compressionThreshold < 0) {
            out.write(VarInt.encode(raw.length));
            out.write(raw);
        } else if (raw.length >= compressionThreshold && compressionThreshold > 0) {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            DeflaterOutputStream def = new DeflaterOutputStream(compressed);
            def.write(raw); def.close();
            byte[] comp = compressed.toByteArray();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(VarInt.encode(raw.length));
            body.write(comp);
            byte[] bodyBytes = body.toByteArray();
            out.write(VarInt.encode(bodyBytes.length));
            out.write(bodyBytes);
        } else {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(VarInt.encode(0));
            body.write(raw);
            byte[] bodyBytes = body.toByteArray();
            out.write(VarInt.encode(bodyBytes.length));
            out.write(bodyBytes);
        }
        return out.toByteArray();
    }

    public static void writePacket(OutputStream out, int packetId, byte[] payload, int compressionThreshold) throws IOException {
        out.write(frame(packetId, payload, compressionThreshold));
        out.flush();
    }

    public static Packet readPacket(InputStream in, int compressionThreshold) throws IOException {
        int length = VarInt.read(in);
        if (length <= 0) throw new IOException("Empty packet");
        byte[] body = readFully(in, length);
        ByteArrayInputStream bin = new ByteArrayInputStream(body);
        byte[] raw;
        if (compressionThreshold < 0) {
            raw = body;
        } else {
            int[] pos = {0};
            int dataLength = VarInt.decode(body, pos);
            if (dataLength == 0) {
                raw = new byte[body.length - pos[0]];
                System.arraycopy(body, pos[0], raw, 0, raw.length);
            } else {
                InflaterInputStream inf = new InflaterInputStream(new ByteArrayInputStream(body, pos[0], body.length - pos[0]));
                ByteArrayOutputStream expanded = new ByteArrayOutputStream(dataLength);
                byte[] buf = new byte[8192]; int n;
                while ((n = inf.read(buf)) != -1) expanded.write(buf, 0, n);
                raw = expanded.toByteArray();
                if (raw.length != dataLength) throw new IOException("Inflated size mismatch");
            }
        }
        int[] pos = {0};
        int id = VarInt.decode(raw, pos);
        byte[] payload = new byte[raw.length - pos[0]];
        System.arraycopy(raw, pos[0], payload, 0, payload.length);
        return new Packet(id, payload);
    }

    private static byte[] readFully(InputStream in, int len) throws IOException {
        byte[] buf = new byte[len];
        int read = 0;
        while (read < len) {
            int n = in.read(buf, read, len - read);
            if (n == -1) throw new IOException("Connection closed mid-packet");
            read += n;
        }
        return buf;
    }
    private PacketIO() {}
}
