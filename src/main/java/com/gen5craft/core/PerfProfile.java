package com.gen5craft.core;

/** Phase 5: one place that turns Settings into concrete performance budgets.
 *  Weak systems get lower render scale, a hard FPS cap and a static scene;
 *  strong systems get full scale. Used by the 3D preview today and by the
 *  future play renderer, so tuning lives in exactly one tested class. */
public final class PerfProfile {
    public final int renderScaleCapPercent;
    public final int targetFps;
    public final boolean staticScene;
    public final String label;
    private PerfProfile(int scale, int fps, boolean isStatic, String label) {
        renderScaleCapPercent = scale; targetFps = fps; staticScene = isStatic; this.label = label;
    }
    public static PerfProfile from(Settings s) {
        Settings.Quality q = s.effectiveQuality();
        int fps = s.fpsLimit == 0 ? 120 : s.fpsLimit;
        switch (q) {
            case PERFORMANCE: return new PerfProfile(60, Math.min(fps, 30), true, "PERFORMANCE (weak system)");
            case BALANCED: return new PerfProfile(85, Math.min(fps, 60), false, "BALANCED");
            case QUALITY:
            default: return new PerfProfile(100, fps, false, "QUALITY (strong system)");
        }
    }
    public int effectiveScale(Settings s) {
        return Math.max(50, Math.min(s.renderScalePercent, renderScaleCapPercent));
    }
}
