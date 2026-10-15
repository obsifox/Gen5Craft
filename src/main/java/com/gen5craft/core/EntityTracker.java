package com.gen5craft.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Phase 8: live entity tracking from the Play stream.
 *
 *  Handles the two entity lifecycle packets whose layout is stable across
 *  Gen 5: Add Entity (id VarInt, UUID, type VarInt, x/y/z Doubles — the
 *  movement/rotation tail after the position is currently ignored) and
 *  Remove Entities (VarInt count + VarInt ids). Entity types stay numeric
 *  (the synchronized entity_type registry IDs sent by the server). */
public final class EntityTracker {
    public static final class Entity {
        public final int id;
        public final UUID uuid;
        public final int type;
        public double x, y, z;
        Entity(int id, UUID uuid, int type, double x, double y, double z) {
            this.id = id; this.uuid = uuid; this.type = type; this.x = x; this.y = y; this.z = z;
        }
        public String toString() {
            return "entity #" + id + " type=" + type + " at (" + x + ", " + y + ", " + z + ")";
        }
    }

    private final Map<Integer, Entity> byId = new LinkedHashMap<Integer, Entity>();
    private int totalSeen;

    public static int addEntityPacketId(int protocol) { return protocol >= 766 ? 0x01 : -1; }
    public static int removeEntitiesPacketId(int protocol) {
        if (protocol < 766) return -1;
        if (protocol <= 767) return 0x42;
        if (protocol <= 769) return 0x47;
        if (protocol <= 772) return 0x46;
        if (protocol <= 774) return 0x4B;
        if (protocol <= 776) return 0x4D;
        return 0x4E;
    }

    /** Parse an Add Entity payload prefix and register the entity. */
    public Entity onAddEntity(byte[] payload) {
        ChunkDataDecoder.Reader r = new ChunkDataDecoder.Reader(payload);
        int id = r.readVarInt();
        long msb = r.readLong(), lsb = r.readLong();
        int type = r.readVarInt();
        double x = r.readDouble(), y = r.readDouble(), z = r.readDouble();
        Entity e = new Entity(id, new UUID(msb, lsb), type, x, y, z);
        byId.put(id, e);
        totalSeen++;
        return e;
    }

    /** Parse a Remove Entities payload; returns how many known entities went away. */
    public int onRemoveEntities(byte[] payload) {
        ChunkDataDecoder.Reader r = new ChunkDataDecoder.Reader(payload);
        int n = r.readVarInt();
        int removed = 0;
        for (int i = 0; i < n; i++) {
            if (byId.remove(r.readVarInt()) != null) removed++;
        }
        return removed;
    }

    public int count() { return byId.size(); }
    public int totalSeen() { return totalSeen; }
    public Entity get(int id) { return byId.get(id); }
    /** Live entity counts keyed by numeric entity_type registry ID. */
    public Map<Integer, Integer> countByType() {
        Map<Integer, Integer> m = new TreeMap<Integer, Integer>();
        for (Entity e : byId.values()) {
            Integer c = m.get(e.type);
            m.put(e.type, c == null ? 1 : c + 1);
        }
        return m;
    }
    public String summary() {
        if (byId.isEmpty()) return "entities: none tracked";
        StringBuilder sb = new StringBuilder("entities live=" + byId.size() + " (seen " + totalSeen + ") by type:");
        for (Map.Entry<Integer, Integer> e : countByType().entrySet()) {
            sb.append(" ").append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }
}
