package com.dieold.game.world;

/**
 * 4-Way Cardinal Grid Directions for isometric room socket connections.
 */
public enum Direction {
    NORTH(0, 1),
    SOUTH(0, -1),
    EAST(1, 0),
    WEST(-1, 0);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Returns the opposite facing direction required for socket matching:
     * NORTH connects to SOUTH, EAST connects to WEST, and vice versa.
     */
    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
}
