package com.gen5craft.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Generation-5 version table (Minecraft Java Edition).
 *  Generation 5 starts at 1.20.5 (protocol 766, items moved NBT -> Data Components)
 *  and is open-ended until the next foundational break. Annual numbering (26.x)
 *  changed only the numbering scheme, not the foundation, so it stays in Gen 5. */
public final class Gen5Versions {
    public static final class Entry {
        public final String id;
        public final int protocol;      // -1 when not yet verified from primary data
        public final boolean annual;    // 26.x annual numbering
        Entry(String id, int protocol, boolean annual) {
            this.id = id; this.protocol = protocol; this.annual = annual;
        }
    }

    private static final List<Entry> TABLE = new ArrayList<Entry>();
    static {
        add("1.20.5", 766, false); add("1.20.6", 766, false);
        add("1.21", 767, false);   add("1.21.1", 767, false);
        add("1.21.2", 768, false); add("1.21.3", 768, false);
        add("1.21.4", 769, false); add("1.21.5", 770, false);
        add("1.21.6", 771, false); add("1.21.7", 772, false);
        add("1.21.8", 772, false); add("1.21.9", 773, false);
        add("1.21.10", 773, false); add("1.21.11", 774, false);
        // 26.1-26.3: verified present in the official Mojang manifest (26.3 is the
        // latest release, 2026-09-15). Protocol numbers are filled from downloaded
        // data at runtime; -1 means "Gen 5, protocol not yet pinned in this table".
        add("26.1", -1, true); add("26.2", -1, true); add("26.3", -1, true);
    }
    private static void add(String id, int p, boolean a) { TABLE.add(new Entry(id, p, a)); }

    public static List<Entry> all() { return Collections.unmodifiableList(TABLE); }
    public static String latest() { return TABLE.get(TABLE.size() - 1).id; }
    public static String first() { return TABLE.get(0).id; }

    public static Entry find(String id) {
        if (id == null) return null;
        String s = id.trim();
        for (Entry e : TABLE) if (e.id.equals(s)) return e;
        return null;
    }
    /** Index in generation order, or -1 when the version is not in Gen 5 table. */
    public static int indexOf(String id) {
        if (id == null) return -1;
        String s = id.trim();
        for (int i = 0; i < TABLE.size(); i++) if (TABLE.get(i).id.equals(s)) return i;
        return -1;
    }
    public static boolean isGen5(String id) { return indexOf(id) >= 0; }
    /** First table entry using this protocol, or null. Protocols can be shared
     *  by a release and its minor follow-up (e.g. 766 = 1.20.5/1.20.6). */
    public static Entry byProtocol(int protocol) {
        if (protocol < 766) return null;
        for (Entry e : TABLE) if (e.protocol == protocol) return e;
        return null;
    }
    /** true when version a is not newer than version b inside Gen 5. */
    public static boolean notNewerThan(String a, String b) {
        int ia = indexOf(a), ib = indexOf(b);
        return ia >= 0 && ib >= 0 && ia <= ib;
    }
    private Gen5Versions() {}
}
