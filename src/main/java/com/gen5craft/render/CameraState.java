package com.gen5craft.render;

/** Phase 6: camera state for the interactive local play session.
 *  Pure logic (no AWT), so rotation/zoom rules are unit-tested headlessly. */
public final class CameraState {
    public double yaw = 0.6;      // radians, always wrapped to [0, 2*PI)
    public double zoom = 1.0;     // clamped to [0.6, 2.0]
    public void rotate(double delta) {
        yaw += delta;
        double twoPi = Math.PI * 2.0;
        while (yaw >= twoPi) yaw -= twoPi;
        while (yaw < 0) yaw += twoPi;
    }
    public void zoomBy(double factor) {
        zoom *= factor;
        if (zoom < 0.6) zoom = 0.6;
        if (zoom > 2.0) zoom = 2.0;
    }
    public void key(char k) {
        switch (Character.toUpperCase(k)) {
            case 'A': rotate(-0.12); break;
            case 'D': rotate(0.12); break;
            case 'W': zoomBy(1.08); break;
            case 'S': zoomBy(1.0 / 1.08); break;
            default: break;
        }
    }
}
