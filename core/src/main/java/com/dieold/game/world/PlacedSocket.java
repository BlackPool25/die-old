package com.dieold.game.world;

/**
 * An instantiated socket in global world grid coordinates, tracking connection state.
 */
public class PlacedSocket {
    public final Socket socket;
    public final PlacedRoom room;
    public final int worldX;
    public final int worldY;
    public final int connectWorldX;
    public final int connectWorldY;

    public PlacedSocket connectedTo = null;
    public boolean isCapped = false;
    public boolean isDoorClosed = false;

    public PlacedSocket(Socket socket, PlacedRoom room, int worldX, int worldY) {
        this.socket = socket;
        this.room = room;
        this.worldX = worldX;
        this.worldY = worldY;
        this.connectWorldX = worldX + socket.direction.dx;
        this.connectWorldY = worldY + socket.direction.dy;
    }

    public boolean isConnected() {
        return connectedTo != null;
    }

    public boolean isOpen() {
        return !isConnected() && !isCapped;
    }

    public void connect(PlacedSocket other) {
        this.connectedTo = other;
        other.connectedTo = this;
    }

    @Override
    public String toString() {
        return "PlacedSocket{" +
            "id='" + socket.id + '\'' +
            ", worldPos=(" + worldX + "," + worldY + ")" +
            ", connectPos=(" + connectWorldX + "," + connectWorldY + ")" +
            ", dir=" + socket.direction +
            ", connected=" + isConnected() +
            ", capped=" + isCapped +
            '}';
    }
}
