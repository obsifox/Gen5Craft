package com.gen5craft.core;

/** Phase 8: player physics over decoded voxel data.
 *
 *  A 0.6 x 1.8 block player AABB with vanilla-like constants (gravity
 *  0.08/tick^2, jump velocity 0.42, fall-speed clamp), axis-separated
 *  collision against solid blocks. Block access is an interface, so the
 *  same engine runs on a retained ChunkColumn from a server, on a local
 *  world, or on a test fixture. Pure logic — fully unit-tested headlessly.
 *
 *  Simplification (documented): every non-fluid block is a full cube;
 *  slab/stair/fence collision shapes are a later refinement. */
public final class PlayerPhysics {
    public interface BlockAccess { int stateAt(int x, int y, int z); }

    public static final double HALF_WIDTH = 0.3;
    public static final double HEIGHT = 1.8;
    public static final double GRAVITY = 0.08;
    public static final double JUMP_VELOCITY = 0.42;
    public static final double TERMINAL_VELOCITY = -3.92;
    private static final double EPS = 1e-4;

    public double x, y, z;        // feet position (block coordinates)
    public double vx, vy, vz;     // velocity, blocks per tick
    public boolean onGround;

    public PlayerPhysics(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    /** Block access over one retained chunk column (outside = air). */
    public static BlockAccess fromChunk(final ChunkColumn chunk) {
        return new BlockAccess() {
            public int stateAt(int wx, int wy, int wz) {
                int lx = wx - chunk.chunkX * 16, lz = wz - chunk.chunkZ * 16;
                if (lx < 0 || lx > 15 || lz < 0 || lz > 15) return 0;
                return chunk.blockStateAt(lx, wy, lz);
            }
        };
    }

    public boolean collides(BlockAccess access, BlockStateTable table, double px, double py, double pz) {
        int x0 = (int) Math.floor(px - HALF_WIDTH), x1 = (int) Math.floor(px + HALF_WIDTH - EPS);
        int y0 = (int) Math.floor(py), y1 = (int) Math.floor(py + HEIGHT - EPS);
        int z0 = (int) Math.floor(pz - HALF_WIDTH), z1 = (int) Math.floor(pz + HALF_WIDTH - EPS);
        for (int bx = x0; bx <= x1; bx++) for (int by = y0; by <= y1; by++) for (int bz = z0; bz <= z1; bz++) {
            if (table.isSolid(access.stateAt(bx, by, bz))) return true;
        }
        return false;
    }

    /** One 20 Hz tick. moveX/moveZ are desired horizontal speeds (blocks/tick). */
    public void tick(BlockAccess access, BlockStateTable table, double moveX, double moveZ, boolean jump) {
        vx = moveX; vz = moveZ;
        if (jump && onGround) { vy = JUMP_VELOCITY; onGround = false; }
        vy -= GRAVITY;
        if (vy < TERMINAL_VELOCITY) vy = TERMINAL_VELOCITY;

        // X axis
        double nx = x + vx;
        if (!collides(access, table, nx, y, z)) x = nx;
        else if (vx > 0) x = Math.floor(nx + HALF_WIDTH) - HALF_WIDTH - EPS;
        else if (vx < 0) x = Math.floor(nx - HALF_WIDTH) + 1 + HALF_WIDTH + EPS;
        // Z axis
        double nz = z + vz;
        if (!collides(access, table, x, y, nz)) z = nz;
        else if (vz > 0) z = Math.floor(nz + HALF_WIDTH) - HALF_WIDTH - EPS;
        else if (vz < 0) z = Math.floor(nz - HALF_WIDTH) + 1 + HALF_WIDTH + EPS;
        // Y axis
        double ny = y + vy;
        onGround = false;
        if (!collides(access, table, x, ny, z)) y = ny;
        else {
            if (vy <= 0) { y = Math.floor(ny) + 1.0; onGround = true; }
            else y = Math.floor(ny + HEIGHT) - HEIGHT - EPS;
            vy = 0;
        }
    }

    /** Highest standing Y (feet) at a column, scanning down from fromY. */
    public static double findGroundY(BlockAccess access, BlockStateTable table, int bx, int bz, int fromY) {
        for (int by = fromY; by > fromY - 512; by--) {
            if (table.isSolid(access.stateAt(bx, by, bz))) return by + 1.0;
        }
        return Double.NaN;
    }
}
