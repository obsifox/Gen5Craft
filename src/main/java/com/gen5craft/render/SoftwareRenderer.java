package com.gen5craft.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Phase 3: CPU-software voxel renderer (zero dependencies).
 *
 *  Renders a deterministic 16x16 voxel terrain (grass/dirt/stone/sand/water
 *  by height) with an isometric rotating camera into a BufferedImage.
 *  Optimizations for weak AND strong systems:
 *   - the scene (heightmap + block types) is generated once, never per frame;
 *   - the painter order for a given camera step is computed from preallocated
 *     arrays with no per-frame collections or allocations in the hot loop;
 *   - callers render at the Settings render-scale into a reused buffer and
 *     upscale, and the UI caps the animation FPS (static on PERFORMANCE).
 *  The Settings GPU mode selects how the result is presented: CPU-software
 *  forces this rasterizer path; Auto/Integrated/Dedicated keep this software
 *  rasterizer for the voxel pass and let the Java2D pipeline (Direct3D when
 *  enabled at launch) present/scale the frame — the engine adapter hook. */
public final class SoftwareRenderer {
    public static final int SIZE = 16;
    private final int[][] height = new int[SIZE][SIZE];
    private final int[][] block = new int[SIZE][SIZE]; // 0 grass, 1 dirt, 2 stone, 3 sand, 4 water
    private final int[] order = new int[SIZE * SIZE];
    private final int[] xs = new int[4];
    private final int[] ys = new int[4];

    private static final Color SKY_TOP = new Color(38, 74, 128);
    private static final Color SKY_BOTTOM = new Color(196, 132, 74);
    private static final Color[] TOP = {
        new Color(96, 176, 72), new Color(134, 96, 67), new Color(128, 128, 128), new Color(219, 207, 163), new Color(52, 95, 218)};
    private static final Color[] LEFT = {
        new Color(74, 138, 56), new Color(104, 74, 52), new Color(100, 100, 100), new Color(188, 176, 138), new Color(40, 76, 186)};
    private static final Color[] RIGHT = {
        new Color(58, 108, 44), new Color(82, 58, 42), new Color(78, 78, 78), new Color(160, 150, 118), new Color(30, 60, 160)};

    public SoftwareRenderer() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double n = Math.sin(x * 0.55) * 1.6 + Math.cos(z * 0.48) * 1.5 + Math.sin((x + z) * 0.23) * 1.2;
                int h = 4 + (int) Math.round(n);
                if (h < 1) h = 1;
                if (h > 9) h = 9;
                height[x][z] = h;
                block[x][z] = h <= 3 ? 4 : (h == 4 ? 3 : (h <= 6 ? 0 : (h <= 7 ? 1 : 2)));
            }
        }
    }

    public int heightAt(int x, int z) { return height[x][z]; }
    public int blockAt(int x, int z) { return block[x][z]; }
    public int waterColumns() {
        int c = 0;
        for (int x = 0; x < SIZE; x++) for (int z = 0; z < SIZE; z++) if (block[x][z] == 4) c++;
        return c;
    }

    /** Render one frame into {@code img} (reused by callers). angle in radians. */
    public void render(BufferedImage img, double angle) { render(img, angle, 1.0); }

    /** Render with a camera zoom factor (0.6 .. 2.0, clamped by CameraState). */
    public void render(BufferedImage img, double angle, double zoom) {
        int w = img.getWidth(), h = img.getHeight();
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            // Sky (two bands, no gradient object churn per frame beyond this draw)
            g.setColor(SKY_TOP); g.fillRect(0, 0, w, h / 2);
            g.setColor(SKY_BOTTOM); g.fillRect(0, h / 2, w, h - h / 2);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            // Build painter order (back to front) in the preallocated array.
            int n = 0;
            for (int x = 0; x < SIZE; x++) for (int z = 0; z < SIZE; z++) order[n++] = (x << 8) | z;
            // Simple depth sort by rotated depth (insertion sort on 256 items, reused array).
            for (int i = 1; i < n; i++) {
                int v = order[i];
                double dv = depth(v, cos, sin);
                int j = i - 1;
                while (j >= 0 && depth(order[j], cos, sin) > dv) { order[j + 1] = order[j]; j--; }
                order[j + 1] = v;
            }
            int tile = Math.max(4, (int) Math.round(Math.min(w, h) / 22.0 * zoom));
            int cx = w / 2, cy = h / 3;
            for (int i = 0; i < n; i++) {
                int x = (order[i] >> 8) & 0xFF, z = order[i] & 0xFF;
                double rx = (x - 7.5) * cos - (z - 7.5) * sin;
                double rz = (x - 7.5) * sin + (z - 7.5) * cos;
                int sx = cx + (int) Math.round((rx - rz) * tile);
                int sy = cy + (int) Math.round((rx + rz) * tile * 0.5);
                drawColumn(g, sx, sy, tile, height[x][z], block[x][z]);
            }
        } finally { g.dispose(); }
    }

    private double depth(int packed, double cos, double sin) {
        int x = (packed >> 8) & 0xFF, z = packed & 0xFF;
        return (x - 7.5) * sin + (z - 7.5) * cos;
    }

    private void drawColumn(Graphics2D g, int sx, int sy, int tile, int blocks, int type) {
        int tw = tile, th = tile / 2, bh = tile / 2;
        int topY = sy - blocks * bh;
        // right face
        g.setColor(RIGHT[type]);
        xs[0] = sx + tw; ys[0] = topY + th; xs[1] = sx; ys[1] = topY + 2 * th;
        xs[2] = sx; ys[2] = topY + 2 * th + blocks * bh; xs[3] = sx + tw; ys[3] = topY + th + blocks * bh;
        g.fillPolygon(xs, ys, 4);
        // left face
        g.setColor(LEFT[type]);
        xs[0] = sx - tw; ys[0] = topY + th; xs[1] = sx; ys[1] = topY + 2 * th;
        xs[2] = sx; ys[2] = topY + 2 * th + blocks * bh; xs[3] = sx - tw; ys[3] = topY + th + blocks * bh;
        g.fillPolygon(xs, ys, 4);
        // top face
        g.setColor(TOP[type]);
        xs[0] = sx; ys[0] = topY; xs[1] = sx + tw; ys[1] = topY + th;
        xs[2] = sx; ys[2] = topY + 2 * th; xs[3] = sx - tw; ys[3] = topY + th;
        g.fillPolygon(xs, ys, 4);
    }

    public static BufferedImage renderToImage(int w, int h, double angle) {
        return renderToImage(w, h, angle, 1.0);
    }
    public static BufferedImage renderToImage(int w, int h, double angle, double zoom) {
        BufferedImage img = new BufferedImage(Math.max(16, w), Math.max(16, h), BufferedImage.TYPE_INT_RGB);
        new SoftwareRenderer().render(img, angle, zoom);
        return img;
    }
}
