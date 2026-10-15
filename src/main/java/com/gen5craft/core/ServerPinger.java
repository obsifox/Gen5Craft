package com.gen5craft.core;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Server List Ping for Gen-5 servers (handshake -> status request -> JSON).
 *  Works for every Gen-5 version because the status flow predates and survives
 *  the generation; the returned protocol number pins the exact version. */
public final class ServerPinger {
    public static final class Status {
        public String versionName;
        public int protocol = -1;
        public int online = -1;
        public int max = -1;
        public String description = "";
        public long latencyMs;
        public FeatureGate.Profile profile;
    }

    public static Status ping(String address, int timeoutMs) throws Exception {
        String host = address == null ? "" : address.trim();
        int port = 25565;
        int colon = host.lastIndexOf(':');
        if (colon > 0) { port = Integer.parseInt(host.substring(colon + 1)); host = host.substring(0, colon); }
        long start = System.currentTimeMillis();
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            OutputStream rawOut = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(rawOut);
            // Handshake (next state = status), use the Gen-5 entry protocol.
            java.io.ByteArrayOutputStream hb = new java.io.ByteArrayOutputStream();
            hb.write(VarInt.encode(0x00));
            hb.write(VarInt.encode(766));
            writeString(hb, host);
            hb.write((port >> 8) & 0xFF); hb.write(port & 0xFF);
            hb.write(VarInt.encode(1));
            out.write(VarInt.encode(hb.size())); out.write(hb.toByteArray());
            // Status request (empty payload).
            out.write(VarInt.encode(1)); out.write(0x00);
            out.flush();
            InputStream in = socket.getInputStream();
            VarInt.read(in); // packet length
            VarInt.read(in); // packet id (0)
            String json = readString(in);
            Status st = new Status();
            st.latencyMs = System.currentTimeMillis() - start;
            st.versionName = extract(json, "\"version\"", "\"name\": \"");
            String proto = extract(json, "\"version\"", "\"protocol\": ");
            if (proto != null) st.protocol = Integer.parseInt(proto.replaceAll("[^0-9-]", ""));
            String max = extract(json, "\"players\"", "\"max\": ");
            String online = extract(json, "\"players\"", "\"online\": ");
            if (max != null) st.max = Integer.parseInt(max.replaceAll("[^0-9-]", ""));
            if (online != null) st.online = Integer.parseInt(online.replaceAll("[^0-9-]", ""));
            String desc = extract(json, "\"description\"", "\"text\": \"");
            st.description = desc == null ? "" : desc;
            if (st.versionName != null) st.profile = FeatureGate.forServer(st.versionName);
            return st;
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
    private static void writeString(java.io.ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.write(VarInt.encode(b.length), 0, VarInt.encode(b.length).length);
        out.write(b, 0, b.length);
    }
    private static String readString(InputStream in) throws Exception {
        int len = VarInt.read(in);
        byte[] buf = new byte[len];
        int read = 0;
        while (read < len) { int n = in.read(buf, read, len - read); if (n == -1) break; read += n; }
        return new String(buf, StandardCharsets.UTF_8);
    }
    private static String extract(String json, String section, String key) {
        if (json == null) return null;
        int from = section == null ? 0 : json.indexOf(section);
        if (from < 0) return null;
        int k = json.indexOf(key, from);
        if (k < 0) return null;
        int s = k + key.length();
        if (key.endsWith(" ")) { int e = s; while (e < json.length() && (Character.isDigit(json.charAt(e)) || json.charAt(e) == '-')) e++; return json.substring(s, e); }
        int e = json.indexOf('"', s);
        return e < 0 ? null : json.substring(s, e);
    }
    private ServerPinger() {}
}
