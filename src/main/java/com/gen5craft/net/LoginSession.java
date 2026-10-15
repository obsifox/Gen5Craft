package com.gen5craft.net;

import com.gen5craft.core.VarInt;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Phase 4: Gen-5 login -> configuration -> play session (offline-mode).
 *
 *  Flow: Handshake(next=login) -> Login Start -> [Set Compression] ->
 *  Login Success -> Login Acknowledged -> Configuration (Client Information,
 *  registry/tag packets are read and logged; Select Known Packs is answered;
 *  Finish Configuration is acknowledged) -> Play state entered and the first
 *  play packets are read and logged, then the socket closes politely.
 *
 *  Boundary: if the server asks for encryption (online-mode), the session
 *  STOPS and reports that Microsoft/Mojang account authentication is required
 *  — that credential step is never faked or bypassed. Below-protocol-766
 *  (other generations) is rejected before any login is attempted. */
public final class LoginSession {
    public interface Listener { void on(String line); }

    public static final class Result {
        public boolean reachedPlay;
        public boolean encryptionRequired;
        public boolean disconnected;
        public String detail = "";
        public int firstPlayPacketId = -1;
    }

    public static UUID offlineUuid(String username) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
        hash[6] = (byte) ((hash[6] & 0x0f) | 0x30); // version 3
        hash[8] = (byte) ((hash[8] & 0x3f) | 0x80); // IETF variant
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++) msb = (msb << 8) | (hash[i] & 0xffL);
        for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (hash[i] & 0xffL);
        return new UUID(msb, lsb);
    }

    public static byte[] buildLoginStart(String username) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, username);
        UUID uuid = offlineUuid(username);
        writeLong(out, uuid.getMostSignificantBits());
        writeLong(out, uuid.getLeastSignificantBits());
        return out.toByteArray();
    }
    public static byte[] buildHandshake(int protocol, String host, int port) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(VarInt.encode(protocol), 0, VarInt.encode(protocol).length);
        writeString(out, host);
        out.write((port >> 8) & 0xFF); out.write(port & 0xFF);
        out.write(VarInt.encode(2), 0, 1); // next state: login
        return out.toByteArray();
    }
    public static byte[] buildClientInformation(String locale) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, locale == null ? "en_us" : locale);
        out.write(10);                       // view distance
        out.write(VarInt.encode(0), 0, 1);   // chat mode: enabled
        out.write(1);                        // chat colors
        out.write(0x7f);                     // displayed skin parts
        out.write(VarInt.encode(1), 0, 1);   // main hand: right
        out.write(0);                        // text filtering
        out.write(1);                        // allow server listings
        return out.toByteArray();
    }
    public static byte[] buildKnownPacks(String coreVersion) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(VarInt.encode(1), 0, 1);   // one known pack
        writeString(out, "minecraft");
        writeString(out, "core");
        writeString(out, coreVersion == null ? "1.21" : coreVersion);
        return out.toByteArray();
    }

    public Result connect(String address, final int protocol, String serverVersionForPacks,
                          String username, int timeoutMs, final Listener listener) throws Exception {
        final Result result = new Result();
        if (protocol < 766) {
            result.detail = "Rejected: protocol " + protocol + " is below Generation 5 (766+).";
            if (listener != null) listener.on(result.detail);
            return result;
        }
        String host = address == null ? "" : address.trim();
        int port = 25565;
        int colon = host.lastIndexOf(':');
        if (colon > 0) { port = Integer.parseInt(host.substring(colon + 1)); host = host.substring(0, colon); }
        String user = (username == null || username.trim().isEmpty()) ? "Gen5Player" : username.trim();
        Socket socket = new Socket();
        try {
            log(listener, "Connecting to " + host + ":" + port + " with protocol " + protocol + " as " + user + " ...");
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            int[] threshold = {-1};
            PacketIO.writePacket(out, 0x00, buildHandshake(protocol, host, port), -1);
            PacketIO.writePacket(out, 0x00, buildLoginStart(user), -1);
            log(listener, "Handshake + Login Start sent. Waiting for login response...");
            boolean loginDone = false;
            int guard = 0;
            while (!loginDone && guard++ < 20) {
                PacketIO.Packet pkt = PacketIO.readPacket(in, threshold[0]);
                log(listener, "Login packet id=0x" + Integer.toHexString(pkt.id) + " (" + pkt.payload.length + " bytes)");
                if (pkt.id == 0x00) { // Disconnect
                    result.disconnected = true;
                    result.detail = "Server disconnected during login: " + readReason(pkt.payload);
                    log(listener, result.detail);
                    return result;
                } else if (pkt.id == 0x01) { // Encryption Request -> online mode
                    result.encryptionRequired = true;
                    result.detail = "Server is in online-mode and requested encryption. Microsoft/Mojang account authentication is required - stopped at the credential boundary (nothing was faked or bypassed).";
                    log(listener, result.detail);
                    return result;
                } else if (pkt.id == 0x03) { // Set Compression
                    int[] pos = {0};
                    threshold[0] = VarInt.decode(pkt.payload, pos);
                    log(listener, "Compression enabled, threshold=" + threshold[0]);
                } else if (pkt.id == 0x02) { // Login Success
                    PacketIO.writePacket(out, 0x03, new byte[0], threshold[0]); // Login Acknowledged
                    log(listener, "Login Success received; acknowledged. Entering Configuration state...");
                    loginDone = true;
                }
            }
            if (!loginDone) { result.detail = "Login did not complete."; return result; }
            PacketIO.writePacket(out, 0x00, buildClientInformation("en_us"), threshold[0]);
            guard = 0;
            while (guard++ < 400) {
                PacketIO.Packet pkt = PacketIO.readPacket(in, threshold[0]);
                if (pkt.id == 0x02) { // Finish Configuration
                    PacketIO.writePacket(out, 0x03, new byte[0], threshold[0]); // Acknowledge Finish Configuration
                    result.reachedPlay = true;
                    log(listener, "Configuration finished -> PLAY state entered.");
                    // Read the first play packets briefly and log them.
                    socket.setSoTimeout(3000);
                    try {
                        for (int i = 0; i < 5; i++) {
                            PacketIO.Packet play = PacketIO.readPacket(in, threshold[0]);
                            if (result.firstPlayPacketId < 0) result.firstPlayPacketId = play.id;
                            log(listener, "Play packet id=0x" + Integer.toHexString(play.id) + " (" + play.payload.length + " bytes)");
                        }
                    } catch (Exception timeout) {
                        log(listener, "Play stream pause/timeout after entry (session goal reached).");
                    }
                    result.detail = "Reached Play state. First play packet id=0x" + Integer.toHexString(result.firstPlayPacketId) + ". Continuous chunk/gameplay processing builds on this session.";
                    return result;
                } else if (pkt.id == 0x0D || pkt.id == 0x0E) { // Select Known Packs (id varies slightly by minor version)
                    PacketIO.writePacket(out, 0x07, buildKnownPacks(serverVersionForPacks), threshold[0]);
                    log(listener, "Answered Select Known Packs (minecraft:core).");
                } else if (pkt.id == 0x01) { // Disconnect (configuration)
                    result.disconnected = true;
                    result.detail = "Server disconnected during configuration: " + readReason(pkt.payload);
                    log(listener, result.detail);
                    return result;
                } else {
                    // Registry Data / tags / resource packs / keep-alive: read and log, never block.
                    log(listener, "Configuration packet id=0x" + Integer.toHexString(pkt.id) + " (" + pkt.payload.length + " bytes) received and logged.");
                }
            }
            result.detail = "Configuration did not finish within the packet guard.";
            return result;
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private static String readReason(byte[] payload) {
        try {
            int[] pos = {0};
            int len = VarInt.decode(payload, pos);
            return new String(payload, pos[0], Math.min(len, payload.length - pos[0]), StandardCharsets.UTF_8);
        } catch (Exception e) { return "(unparsable reason)"; }
    }
    private static void writeString(ByteArrayOutputStream out, String s) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        byte[] len = VarInt.encode(b.length);
        out.write(len, 0, len.length);
        out.write(b, 0, b.length);
    }
    private static void writeLong(ByteArrayOutputStream out, long v) {
        for (int i = 7; i >= 0; i--) out.write((int) ((v >> (i * 8)) & 0xFF));
    }
    private static void log(Listener l, String s) { if (l != null) l.on(s); }
}
