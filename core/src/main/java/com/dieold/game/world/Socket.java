package com.dieold.game.world;

import com.badlogic.gdx.math.Vector2;

/**
 * Socket metadata defining a directional doorway connection on a RoomTemplate.
 */
public class Socket {
    public final String id;
    public final int x;
    public final int y;
    public final Direction direction;
    public final SocketType type;
    public final int clearanceWidth;

    public Socket(String id, int x, int y, Direction direction, SocketType type, int clearanceWidth) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.type = type;
        this.clearanceWidth = clearanceWidth;
    }

    public Socket(String id, int x, int y, Direction direction, SocketType type) {
        this(id, x, y, direction, type, 2);
    }

    /**
     * Checks if two sockets can connect: opposite direction, matching type, and matching clearance width.
     */
    public boolean isCompatible(Socket other) {
        if (other == null) return false;
        return this.direction == other.direction.opposite()
            && this.type == other.type
            && this.clearanceWidth == other.clearanceWidth;
    }

    public Vector2 getLocalPosition(Vector2 out) {
        out.set(x, y);
        return out;
    }

    @Override
    public String toString() {
        return "Socket{" +
            "id='" + id + '\'' +
            ", pos=(" + x + "," + y + ")" +
            ", dir=" + direction +
            ", type=" + type +
            ", width=" + clearanceWidth +
            '}';
    }
}
