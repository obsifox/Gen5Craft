package com.gen5craft;

import com.gen5craft.core.Settings;
import com.gen5craft.ui.MainMenuFrame;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Gen5Craft entry point (Windows-first, Java).
 *  Graphics pipeline flags must be set before AWT starts: Dedicated/Auto keep
 *  the Direct3D pipeline, CPU-software mode disables hardware pipelines so the
 *  menu stays smooth even on machines without a usable GPU. The selected mode
 *  is also stored for the 3D engine adapter selection. */
public final class Main {
    public static Path gameDir() {
        String appData = System.getenv("APPDATA");
        if (System.getProperty("os.name", "").toLowerCase().contains("win") && appData != null && !appData.isEmpty()) {
            return Paths.get(appData, "Gen5Craft");
        }
        return Paths.get(System.getProperty("user.home"), ".gen5craft");
    }

    public static void main(String[] args) {
        Path dir = gameDir();
        Path settingsFile = dir.resolve("gen5craft.properties");
        Settings settings = Settings.load(settingsFile);
        applyPipeline(settings.gpuMode);
        final Settings s = settings;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new MainMenuFrame(s, settingsFile, dir).setVisible(true); }
        });
    }

    static void applyPipeline(Settings.GpuMode mode) {
        if (mode == Settings.GpuMode.CPU_SOFTWARE) {
            System.setProperty("sun.java2d.d3d", "false");
            System.setProperty("sun.java2d.opengl", "false");
            System.setProperty("sun.java2d.xrender", "false");
            System.setProperty("sun.java2d.noddraw", "true");
        } else {
            System.setProperty("sun.java2d.d3d", "true");
        }
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }
    private Main() {}
}
