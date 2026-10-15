package com.gen5craft.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** User settings, including the graphics (GPU) section.
 *  GPU mode is consumed in two places: (1) the Java2D pipeline flags applied at
 *  launch time, and (2) the 3D engine adapter selection (Auto / integrated CPU
 *  graphics / dedicated GPU / CPU software rendering as fallback). */
public final class Settings {
    public enum GpuMode { AUTO, INTEGRATED, DEDICATED, CPU_SOFTWARE }
    public enum Quality { AUTO, PERFORMANCE, BALANCED, QUALITY }

    public GpuMode gpuMode = GpuMode.AUTO;
    public Quality quality = Quality.AUTO;
    public int renderScalePercent = 100;
    public int fpsLimit = 60;
    public boolean panoramaAnimation = true;
    public String language = "fa";

    public static Settings load(Path file) {
        Settings s = new Settings();
        if (file == null || !Files.exists(file)) return s;
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = Files.newInputStream(file);
            p.load(in);
            s.gpuMode = GpuMode.valueOf(p.getProperty("gpuMode", "AUTO"));
            s.quality = Quality.valueOf(p.getProperty("quality", "AUTO"));
            s.renderScalePercent = clamp(parseInt(p.getProperty("renderScalePercent"), 100), 50, 100);
            s.fpsLimit = parseInt(p.getProperty("fpsLimit"), 60);
            s.panoramaAnimation = Boolean.parseBoolean(p.getProperty("panoramaAnimation", "true"));
            s.language = p.getProperty("language", "fa");
        } catch (Exception ignored) {
            return new Settings();
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
        }
        return s;
    }

    public void save(Path file) throws IOException {
        Properties p = new Properties();
        p.setProperty("gpuMode", gpuMode.name());
        p.setProperty("quality", quality.name());
        p.setProperty("renderScalePercent", Integer.toString(renderScalePercent));
        p.setProperty("fpsLimit", Integer.toString(fpsLimit));
        p.setProperty("panoramaAnimation", Boolean.toString(panoramaAnimation));
        p.setProperty("language", language);
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        OutputStream out = Files.newOutputStream(file);
        try { p.store(out, "Gen5Craft settings"); } finally { out.close(); }
    }

    /** Effective profile for this machine: weak systems get the static panorama,
     *  reduced render scale and the software-friendly pipeline automatically. */
    public Quality effectiveQuality() {
        if (quality != Quality.AUTO) return quality;
        int cores = Runtime.getRuntime().availableProcessors();
        long maxMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        if (cores <= 2 || maxMb <= 1024) return Quality.PERFORMANCE;
        if (cores <= 4 || maxMb <= 2048) return Quality.BALANCED;
        return Quality.QUALITY;
    }

    public String systemSummary() {
        return Runtime.getRuntime().availableProcessors() + " CPU threads, max heap "
            + (Runtime.getRuntime().maxMemory() / (1024L * 1024L)) + " MB, Java "
            + System.getProperty("java.version") + ", OS " + System.getProperty("os.name");
    }

    private static int parseInt(String v, int d) {
        try { return Integer.parseInt(v); } catch (Exception e) { return d; }
    }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
