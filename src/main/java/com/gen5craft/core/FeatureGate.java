package com.gen5craft.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Content introduced per Gen-5 version. When the client (built on the latest
 *  data, 26.3) joins an older Gen-5 server, everything introduced after that
 *  server's version is disabled for the user. Single-player/maps always run
 *  on the latest data and never disable anything. */
public final class FeatureGate {
    private static final Map<String, String[]> INTRODUCED = new LinkedHashMap<String, String[]>();
    static {
        INTRODUCED.put("1.20.5", new String[]{"data-components-item-model", "wolf-variants", "armadillo"});
        INTRODUCED.put("1.21", new String[]{"trial-chambers", "mace", "ominous-trials", "crafter"});
        INTRODUCED.put("1.21.2", new String[]{"bundle-full-release", "salmon-size-variants"});
        INTRODUCED.put("1.21.4", new String[]{"pale-garden", "creaking"});
        INTRODUCED.put("1.21.5", new String[]{"animal-variants-temperature", "falling-leaf-particles"});
        INTRODUCED.put("1.21.6", new String[]{"happy-ghast", "locator-bar"});
        INTRODUCED.put("1.21.9", new String[]{"copper-golem", "copper-chest", "shelf"});
        INTRODUCED.put("26.1", new String[]{"annual-numbering-content-set"});
        INTRODUCED.put("26.2", new String[]{"annual-26.2-content-set"});
        INTRODUCED.put("26.3", new String[]{"annual-26.3-content-set"});
    }

    public static final class Profile {
        public final String targetVersion;
        public final List<String> disabledFeatures;
        public final boolean allowed;
        public final String reason;
        Profile(String t, List<String> d, boolean a, String r) {
            targetVersion = t; disabledFeatures = d; allowed = a; reason = r;
        }
    }

    /** Build the effective profile for a server running {@code serverVersion}. */
    public static Profile forServer(String serverVersion) {
        int target = Gen5Versions.indexOf(serverVersion);
        if (target < 0) {
            return new Profile(serverVersion, new ArrayList<String>(), false,
                "Outside Generation 5 (1.20.5-26.3): foundational structure differs, simple disabling is not enough.");
        }
        List<String> disabled = new ArrayList<String>();
        for (Map.Entry<String, String[]> e : INTRODUCED.entrySet()) {
            if (Gen5Versions.indexOf(e.getKey()) > target) {
                for (String f : e.getValue()) disabled.add(f);
            }
        }
        return new Profile(serverVersion, disabled, true,
            disabled.isEmpty() ? "Server is on the latest Gen-5 data: nothing disabled."
                               : disabled.size() + " newer feature(s) disabled for this server.");
    }

    /** Local worlds/maps: always latest, nothing disabled. */
    public static Profile forLocalWorld() {
        return new Profile(Gen5Versions.latest(), new ArrayList<String>(), true,
            "Local world runs on the latest Gen-5 data (" + Gen5Versions.latest() + ").");
    }
    private FeatureGate() {}
}
