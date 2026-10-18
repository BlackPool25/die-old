package com.dieold.game.world;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.dieold.game.combat.ScreenShake;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.EntityFactory;
import com.dieold.game.ecs.components.HealthComponent;
import com.dieold.game.physics.PhysicsConstants;

/**
 * Chamber Lockdown & Portcullis Gates Manager:
 * Detects player crossing doorway thresholds into uncleared combat chambers,
 * dynamically activates Box2D barrier colliders, triggers gate slam VFX + screen shake,
 * spawns room encounters, tracks room wipe, and removes barriers to restore passage.
 */
public class RoomLockdownManager {

    public interface LockdownEventListener {
        void onLockdownTriggered(PlacedRoom room);
        void onLockdownCleared(PlacedRoom room);
    }

    private final DungeonLevel dungeonLevel;
    private final World world;
    private final EntityFactory entityFactory;
    private final ScreenShake screenShake;

    private PlacedRoom activeLockdownRoom = null;
    private LockdownEventListener listener = null;

    private final PolygonShape gateShape = new PolygonShape();
    private final FixtureDef gateFixDef = new FixtureDef();
    private final BodyDef gateBodyDef = new BodyDef();

    public RoomLockdownManager(
        DungeonLevel dungeonLevel,
        World world,
        EntityFactory entityFactory,
        ScreenShake screenShake
    ) {
        this.dungeonLevel = dungeonLevel;
        this.world = world;
        this.entityFactory = entityFactory;
        this.screenShake = screenShake;

        gateShape.setAsBox(0.48f, 0.48f);
        gateFixDef.shape = gateShape;
        gateFixDef.friction = 0.5f;
        gateFixDef.restitution = 0.1f;
        gateFixDef.filter.categoryBits = PhysicsConstants.CATEGORY_WALL;
        gateFixDef.filter.maskBits = PhysicsConstants.MASK_WALL;

        gateBodyDef.type = BodyDef.BodyType.StaticBody;
    }

    public void setEventListener(LockdownEventListener listener) {
        this.listener = listener;
    }

    public void update(float deltaTime, float playerX, float playerY) {
        PlacedRoom currentRoom = dungeonLevel.getRoomAt(playerX, playerY);

        // 1. Check for doorway threshold entry into an uncleared combat arena
        if (activeLockdownRoom == null) {
            if (currentRoom != null && currentRoom.template.isCombatRoom && currentRoom.state == PlacedRoom.RoomState.UNVISITED) {
                // Must be safely inside room bounds (1 tile past outer doorway threshold)
                if (isInsideRoomInterior(currentRoom, playerX, playerY)) {
                    triggerLockdown(currentRoom);
                }
            }
        } else {
            // 2. Track room wipe for active lockdown
            checkRoomWipe();
        }
    }

    private boolean isInsideRoomInterior(PlacedRoom room, float px, float py) {
        float margin = 1.2f;
        return px >= (room.gridX + margin) && px <= (room.gridX + room.template.width - margin)
            && py >= (room.gridY + margin) && py <= (room.gridY + room.template.height - margin);
    }

    public void triggerLockdown(PlacedRoom room) {
        if (room == null || room.state == PlacedRoom.RoomState.CLEARED) return;

        room.state = PlacedRoom.RoomState.ACTIVE_LOCKDOWN;
        activeLockdownRoom = room;

        // 1. Spawn dynamic Box2D barrier fixtures blocking all entrance/exit sockets
        for (int s = 0; s < room.sockets.size; s++) {
            PlacedSocket ps = room.sockets.get(s);
            if (ps.isConnected()) {
                ps.isDoorClosed = true;

                int width = ps.socket.clearanceWidth;
                for (int k = 0; k < width; k++) {
                    int gx = ps.worldX;
                    int gy = ps.worldY;
                    if (ps.socket.direction == Direction.NORTH || ps.socket.direction == Direction.SOUTH) {
                        gx += k;
                    } else {
                        gy += k;
                    }

                    gateBodyDef.position.set(gx + 0.5f, gy + 0.5f);
                    Body gateBody = world.createBody(gateBodyDef);
                    gateBody.createFixture(gateFixDef);
                    gateBody.setUserData("WALL");
                    room.gateBodies.add(gateBody);

                    // Gate slam dust puff VFX
                    if (entityFactory != null) {
                        entityFactory.createVFX("dust_puff", gx + 0.5f, gy + 0.5f, 90f, 1.2f);
                    }
                }
            }
        }

        // 2. Screen shake impulse
        if (screenShake != null) {
            screenShake.addShake(8.0f, 0.45f);
        }

        // 3. Spawn room enemies if entityFactory available
        if (entityFactory != null) {
            for (int i = 0; i < room.template.enemySpawns.size; i++) {
                Vector2 spawn = room.template.enemySpawns.get(i);
                Entity hound = entityFactory.createFoundryHound(room.gridX + spawn.x, room.gridY + spawn.y);
                room.activeEnemies.add(hound);
            }
        }

        if (listener != null) {
            listener.onLockdownTriggered(room);
        }
    }

    private void checkRoomWipe() {
        if (activeLockdownRoom == null) return;

        // Remove dead enemies
        for (int i = activeLockdownRoom.activeEnemies.size - 1; i >= 0; i--) {
            Entity e = activeLockdownRoom.activeEnemies.get(i);
            HealthComponent health = ComponentMappers.health.get(e);
            if (health == null || health.isDead()) {
                activeLockdownRoom.activeEnemies.removeIndex(i);
            }
        }

        // If all enemies wiped, lift lockdown!
        if (activeLockdownRoom.activeEnemies.size == 0) {
            liftLockdown(activeLockdownRoom);
        }
    }

    public void liftLockdown(PlacedRoom room) {
        if (room == null) return;

        room.state = PlacedRoom.RoomState.CLEARED;

        // 1. Remove Box2D barrier fixtures
        for (int i = 0; i < room.gateBodies.size; i++) {
            Body b = room.gateBodies.get(i);
            world.destroyBody(b);
        }
        room.gateBodies.clear();

        // 2. Re-open doors and play unlock VFX
        for (int s = 0; s < room.sockets.size; s++) {
            PlacedSocket ps = room.sockets.get(s);
            if (ps.isConnected()) {
                ps.isDoorClosed = false;

                if (entityFactory != null) {
                    entityFactory.createVFX("dust_puff", ps.worldX + 0.5f, ps.worldY + 0.5f, 0f, 0.8f);
                }
            }
        }

        // 3. Unlock screen shake impulse
        if (screenShake != null) {
            screenShake.addShake(4.0f, 0.25f);
        }

        activeLockdownRoom = null;

        if (listener != null) {
            listener.onLockdownCleared(room);
        }
    }

    public PlacedRoom getActiveLockdownRoom() {
        return activeLockdownRoom;
    }

    public boolean isLockdownActive() {
        return activeLockdownRoom != null;
    }

    public void dispose() {
        gateShape.dispose();
    }
}
