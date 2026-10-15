package com.gen5craft.launcher;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Skin texture provider for the profile card. Priority:
 *  1. the profile's custom 64x64 skin PNG (user file),
 *  2. the game's own default Steve: Steve.png extracted from the official
 *     client jar the user downloaded via Mojang's manifest
 *     (gameDir/versions/<version>/<version>.jar),
 *  3. a built-in Steve-style fallback drawn in code (used only until the
 *     game data is downloaded).
 *  Head rendering is an isometric cube (top/front/side + hat overlay). */
public final class SkinTextures {
    private SkinTextures() {}

    public static final class Loaded {
        public final BufferedImage skin;   // 64x64
        public final String source;        // human-readable origin
        Loaded(BufferedImage skin, String source) { this.skin = skin; this.source = source; }
    }

    public static Loaded loadSkin(Path gameDir, Profile profile) {
        // 1) Custom skin file.
        if (profile != null && profile.skinPath != null && !profile.skinPath.trim().isEmpty()) {
            try {
                BufferedImage img = ImageIO.read(new File(profile.skinPath.trim()));
                if (img != null && img.getWidth() >= 64 && img.getHeight() >= 32) {
                    return new Loaded(normalize(img), "custom skin file");
                }
            } catch (Exception ignored) {}
        }
        // 2) Default Steve from the downloaded official client jar.
        try {
            Path jar = findClientJar(gameDir);
            if (jar != null) {
                ZipFile zip = new ZipFile(jar.toFile());
                try {
                    ZipEntry e = findSteveEntry(zip);
                    if (e != null) {
                        InputStream in = zip.getInputStream(e);
                        BufferedImage img = ImageIO.read(in);
                        in.close();
                        if (img != null) return new Loaded(normalize(img), "default Steve from " + jar.getFileName());
                    }
                } finally { zip.close(); }
            }
        } catch (Exception ignored) {}
        // 3) Built-in fallback.
        return new Loaded(builtInSteve(), "built-in Steve (download game data for the real texture)");
    }

    private static Path findClientJar(Path gameDir) {
        if (gameDir == null) return null;
        Path versions = gameDir.resolve("versions");
        if (!Files.isDirectory(versions)) return null;
        Path best = null;
        try {
            DirectoryStream<Path> ds = Files.newDirectoryStream(versions);
            try {
                for (Path dir : ds) {
                    if (!Files.isDirectory(dir)) continue;
                    Path jar = dir.resolve(dir.getFileName().toString() + ".jar");
                    if (Files.exists(jar)) {
                        if (dir.getFileName().toString().equals("26.3")) return jar; // latest first
                        if (best == null) best = jar;
                    }
                }
            } finally { ds.close(); }
        } catch (Exception ignored) {}
        return best;
    }
    private static ZipEntry findSteveEntry(ZipFile zip) {
        ZipEntry fallback = null;
        java.util.Enumeration<? extends ZipEntry> en = zip.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            String n = e.getName();
            if (n.endsWith("textures/entity/player/wide/Steve.png")) return e;
            if (n.endsWith("textures/entity/Steve.png")) fallback = e;
        }
        return fallback;
    }

    /** Force any skin (incl. legacy 64x32) onto a 64x64 canvas. */
    public static BufferedImage normalize(BufferedImage img) {
        if (img.getWidth() == 64 && img.getHeight() == 64 && img.getType() == BufferedImage.TYPE_INT_ARGB) return img;
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Head face (8x8 at (8,8)) with the hat overlay (40,8) composited. */
    public static BufferedImage headFace(BufferedImage skin) {
        BufferedImage face = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = face.createGraphics();
        g.drawImage(skin.getSubimage(8, 8, 8, 8), 0, 0, null);
        g.drawImage(skin.getSubimage(40, 8, 8, 8), 0, 0, null);
        g.dispose();
        return face;
    }

    /** Isometric head cube (top / front / side, hat overlay, face shading). */
    public static BufferedImage headCube(BufferedImage skin, int size) {
        int s = Math.max(24, size);
        BufferedImage out = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // Isometric cube geometry (2:1 dimetric). The cube spans 84% of the
        // canvas width: edge projections e1 (right-down), e2 (left-down) and
        // e3 (straight down). Each AffineTransform below maps the texture's
        // unit square onto one face of the cube; drawFace scales pixels to it.
        double cx = s / 2.0, topY = s * 0.06;
        double dx = s * 0.42, dy = dx * 0.5, down = dx * 1.05;
        // face textures: top (8,0), front (8,8), side (16,8); hat: (40,0), (40,8), (48,8)
        AffineTransform top = new AffineTransform(dx, dy, -dx, dy, cx, topY);
        AffineTransform side = new AffineTransform(dx, -dy, 0, down, cx, topY + 2 * dy);
        AffineTransform front = new AffineTransform(dx, dy, 0, down, cx - dx, topY + dy);
        drawFace(g, shade(skin.getSubimage(8, 0, 8, 8), 1.0), top);
        drawFace(g, shade(skin.getSubimage(16, 8, 8, 8), 0.62), side);
        drawFace(g, shade(skin.getSubimage(8, 8, 8, 8), 0.82), front);
        drawFace(g, skin.getSubimage(40, 0, 8, 8), top);
        drawFace(g, skin.getSubimage(48, 8, 8, 8), side);
        drawFace(g, skin.getSubimage(40, 8, 8, 8), front);
        g.dispose();
        return out;
    }
    private static void drawFace(Graphics2D g, BufferedImage tex, AffineTransform unit) {
        AffineTransform t = new AffineTransform(unit);
        t.scale(1.0 / 8.0, 1.0 / 8.0);
        g.drawImage(tex, t, null);
    }
    private static BufferedImage shade(BufferedImage src, double f) {
        if (f >= 0.999) return src;
        BufferedImage out = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            int argb = src.getRGB(x, y);
            int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF, gg = (argb >> 8) & 0xFF, b = argb & 0xFF;
            out.setRGB(x, y, (a << 24) | ((int) (r * f) << 16) | ((int) (gg * f) << 8) | (int) (b * f));
        }
        return out;
    }

    /** Small cape preview swatch for the given cape (cosmetic, offline). */
    public static BufferedImage capePreview(Profile.Cape cape, int w, int h) {
        BufferedImage out = new BufferedImage(Math.max(8, w), Math.max(12, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        if (cape != null && cape != Profile.Cape.NONE) {
            g.setColor(cape.color);
            g.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);
            g.setColor(new Color(Math.min(255, cape.color.getRed() + 50), Math.min(255, cape.color.getGreen() + 50), Math.min(255, cape.color.getBlue() + 50)));
            g.drawRoundRect(2, 2, w - 5, h - 5, 5, 5);
            int cx = w / 2, cy = h / 2, r = Math.min(w, h) / 5;
            g.fillPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);
        }
        g.dispose();
        return out;
    }

    /** Built-in Steve-style skin (only head regions are used by the card).
     *  Colors approximate the classic default Steve: brown hair, tan skin,
     *  white/violet eyes, teal shirt, blue-violet trousers. */
    public static BufferedImage builtInSteve() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Color hair = new Color(0x3B2A1A), skin = new Color(0xB07D5A), skinDark = new Color(0x96684A);
        Color eyeW = new Color(0xFFFFFF), eyeI = new Color(0x4632A8), mouth = new Color(0x7A4B32);
        // head top (8,0,8,8) all hair; head bottom (16,0) skin
        fill(img, 8, 0, 8, 8, hair); fill(img, 16, 0, 8, 8, skin);
        // sides (0,8) and (16,8), back (24,8): hair on top half, skin below
        for (int rx : new int[]{0, 16, 24}) { fill(img, rx, 8, 8, 4, hair); fill(img, rx, 12, 8, 4, skin); }
        // face (8,8,8,8)
        fill(img, 8, 8, 8, 8, skin);
        fill(img, 8, 8, 8, 1, hair);             // fringe row
        // eyes on face row 3: white+iris at x 1-2 and 5-6
        img.setRGB(9, 11, eyeW.getRGB()); img.setRGB(10, 11, eyeI.getRGB());
        img.setRGB(13, 11, eyeI.getRGB()); img.setRGB(14, 11, eyeW.getRGB());
        fill(img, 11, 12, 2, 2, skinDark);       // nose
        fill(img, 11, 14, 2, 1, mouth);          // mouth
        fill(img, 8, 15, 8, 1, skinDark);        // jaw shade
        // body: teal shirt (20,20,8,12), arms skin/shirt, legs blue-violet
        Color shirt = new Color(0x00A8A8), legs = new Color(0x4040C8);
        fill(img, 20, 20, 8, 12, shirt); fill(img, 20, 36, 8, 12, shirt);
        fill(img, 4, 20, 4, 12, shirt); fill(img, 4, 32, 4, 4, skin);
        fill(img, 32, 20, 4, 12, shirt); fill(img, 32, 32, 4, 4, skin);
        fill(img, 4, 52, 4, 12, legs); fill(img, 20, 52, 8, 12, legs);
        return img;
    }
    private static void fill(BufferedImage img, int x, int y, int w, int h, Color c) {
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(x, y, w, h);
        g.dispose();
    }
}
