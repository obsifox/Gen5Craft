package com.gen5craft;

import com.gen5craft.core.FeatureGate;
import com.gen5craft.core.Gen5Versions;
import com.gen5craft.core.VarInt;

/** Dependency-free test harness (no JUnit download needed on CI). */
public final class TestMain {
    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
        System.out.println("PASS: " + msg);
    }
    public static void main(String[] args) {
        check(Gen5Versions.isGen5("1.20.5"), "1.20.5 is Gen5 (first)");
        check(Gen5Versions.isGen5("26.3"), "26.3 is Gen5 (latest)");
        check(!Gen5Versions.isGen5("1.20.4"), "1.20.4 is NOT Gen5 (previous generation)");
        check(!Gen5Versions.isGen5("1.12.2"), "1.12.2 is NOT Gen5");
        check(Gen5Versions.latest().equals("26.3"), "latest is 26.3");
        FeatureGate.Profile latest = FeatureGate.forServer("26.3");
        check(latest.allowed && latest.disabledFeatures.isEmpty(), "26.3 server disables nothing");
        FeatureGate.Profile older = FeatureGate.forServer("1.20.5");
        check(older.allowed && !older.disabledFeatures.isEmpty(), "1.20.5 server disables newer content");
        check(older.disabledFeatures.contains("mace"), "mace (1.21) disabled on 1.20.5 server");
        check(!FeatureGate.forServer("1.20.4").allowed, "1.20.4 server rejected as other generation");
        check(FeatureGate.forLocalWorld().disabledFeatures.isEmpty(), "local world disables nothing");
        int[] values = {0, 1, 127, 128, 766, 774, 25565, Integer.MAX_VALUE};
        for (int v : values) {
            int[] pos = {0};
            check(VarInt.decode(VarInt.encode(v), pos) == v, "VarInt roundtrip " + v);
        }
        check(Gen5Versions.byProtocol(766) != null && Gen5Versions.byProtocol(766).id.equals("1.20.5"), "protocol 766 -> 1.20.5");
        check(Gen5Versions.byProtocol(774) != null && Gen5Versions.byProtocol(774).id.equals("1.21.11"), "protocol 774 -> 1.21.11");
        check(Gen5Versions.byProtocol(765) == null, "protocol 765 (1.20.4) is previous generation");
        System.out.println("ALL TESTS PASSED");
    }
}
