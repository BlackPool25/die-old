package com.dieold.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Room-to-Room Fog of War Manager:
 * Cloaks unvisited rooms in deep obsidian darkness (#040406).
 * Reveals newly entered chambers with a smooth 0.4-second alpha fade.
 */
public class FogOfWarSystem {

    public static final Color OBSIDIAN_DARKNESS = new Color(0.04f, 0.04f, 0.06f, 1.0f);
    public static final float REVEAL_DURATION = 0.40f; // 0.4 second smooth fade

    private final DungeonLevel dungeonLevel;
    private final Color tempColor = new Color();

    public FogOfWarSystem(DungeonLevel dungeonLevel) {
        this.dungeonLevel = dungeonLevel;
    }

    public void update(float deltaTime, float playerX, float playerY) {
        Array<PlacedRoom> rooms = dungeonLevel.getRooms();
        if (rooms == null) return;

        // 1. Identify current room and adjacent chambers via connected sockets
        PlacedRoom currentRoom = dungeonLevel.getRoomAt(playerX, playerY);
        if (currentRoom != null) {
            discoverRoom(currentRoom);

            // Also preview/discover adjacent room if player is near connected doorway (within 2.5 meters)
            for (int s = 0; s < currentRoom.sockets.size; s++) {
                PlacedSocket ps = currentRoom.sockets.get(s);
                if (ps.isConnected()) {
                    float distSq = MathUtils.clamp(
                        (playerX - ps.worldX) * (playerX - ps.worldX) + (playerY - ps.worldY) * (playerY - ps.worldY),
                        0f, Float.MAX_VALUE
                    );
                    if (distSq < 6.25f) { // 2.5 meters squared
                        discoverRoom(ps.connectedTo.room);
                    }
                }
            }
        }

        // 2. Animate discovery alpha transitions
        for (int i = 0; i < rooms.size; i++) {
            PlacedRoom r = rooms.get(i);
            if (r.state != PlacedRoom.RoomState.UNVISITED && r.discoveryAlpha < 1.0f) {
                r.discoveryAlpha = Math.min(1.0f, r.discoveryAlpha + (deltaTime / REVEAL_DURATION));
            }
        }
    }

    public void discoverRoom(PlacedRoom room) {
        if (room != null && room.state == PlacedRoom.RoomState.UNVISITED) {
            room.state = PlacedRoom.RoomState.DISCOVERED;
        }
    }

    /**
     * Gets the current visual tint color for a given room.
     */
    public Color getRoomTint(PlacedRoom room, Color out) {
        if (room == null || room.state == PlacedRoom.RoomState.UNVISITED) {
            return out.set(OBSIDIAN_DARKNESS);
        }
        float a = room.discoveryAlpha;
        out.r = MathUtils.lerp(OBSIDIAN_DARKNESS.r, 1.0f, a);
        out.g = MathUtils.lerp(OBSIDIAN_DARKNESS.g, 1.0f, a);
        out.b = MathUtils.lerp(OBSIDIAN_DARKNESS.b, 1.0f, a);
        out.a = 1.0f;
        return out;
    }
}
