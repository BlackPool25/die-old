package com.dieold.game;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.dieold.game.combat.ScreenShake;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.EntityFactory;
import com.dieold.game.ecs.components.HealthComponent;
import com.dieold.game.physics.PhysicsConstants;
import com.dieold.game.world.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless test validating Chamber Lockdown & Portcullis Gates:
 * 1. Dynamic Box2D barrier fixtures activate upon entering uncleared combat room.
 * 2. Barriers block Box2D raycasts and physical passage (CATEGORY_WALL).
 * 3. Defeating all room enemies lifts lockdown, destroying barrier bodies and restoring passage.
 */
public class ChamberLockdownTest {

    @BeforeAll
    public static void initBox2D() {
        Box2D.init();
    }

    @Test
    @DisplayName("Verify chamber lockdown gate activation, collision blocking, and room wipe deactivation")
    public void testChamberLockdownLifecycle() {
        World world = new World(new Vector2(0f, 0f), true);
        Engine engine = new Engine();
        EntityFactory factory = new EntityFactory(engine, world);
        ScreenShake screenShake = new ScreenShake();

        // 1. Generate small 2-room layout: IngressHub connected to LockdownChamber
        RoomPrefabRegistry registry = new RoomPrefabRegistry();
        RoomTemplate ingressT = registry.getTemplate("IngressHub");
        RoomTemplate combatT = registry.getTemplate("LockdownChamber");

        PlacedRoom ingress = new PlacedRoom(ingressT, 0, 0);
        // North socket of IngressHub at (7, 15) connects to South socket of LockdownChamber at (7, 0)
        // Placement formula: P_room = (0 + 7, 0 + 15 + 1) - (7, 0) = (0, 16)
        PlacedRoom combatRoom = new PlacedRoom(combatT, 0, 16);

        // Connect sockets
        PlacedSocket parentSocket = ingress.sockets.get(0); // ingress_n
        PlacedSocket childSocket = combatRoom.sockets.get(0); // lockdown_s
        parentSocket.connect(childSocket);

        Array<PlacedRoom> rooms = new Array<>();
        rooms.add(ingress);
        rooms.add(combatRoom);

        DungeonLevel level = new DungeonLevel();
        // Null atlas is fine for headless collision test
        // Let's create dummy Mock TextureAtlas or build without atlas
        // Alternatively, use AssetResolver if atlas is needed, or instantiate bodies directly
        RoomLockdownManager lockdownManager = new RoomLockdownManager(level, world, factory, screenShake);

        // Verify initial state
        assertEquals(PlacedRoom.RoomState.CLEARED, ingress.state);
        assertEquals(PlacedRoom.RoomState.UNVISITED, combatRoom.state);
        assertEquals(0, combatRoom.gateBodies.size);

        // 2. Trigger lockdown on combat room
        lockdownManager.triggerLockdown(combatRoom);

        assertTrue(lockdownManager.isLockdownActive());
        assertEquals(combatRoom, lockdownManager.getActiveLockdownRoom());
        assertEquals(PlacedRoom.RoomState.ACTIVE_LOCKDOWN, combatRoom.state);
        assertTrue(combatRoom.gateBodies.size > 0, "Gate barrier bodies must be spawned on lockdown");

        // Verify gate barrier bodies have CATEGORY_WALL
        for (int i = 0; i < combatRoom.gateBodies.size; i++) {
            Body gateBody = combatRoom.gateBodies.get(i);
            Fixture fix = gateBody.getFixtureList().first();
            assertEquals(PhysicsConstants.CATEGORY_WALL, fix.getFilterData().categoryBits,
                "Gate barrier fixture must have CATEGORY_WALL");
        }

        // Raycast across the doorway to verify obstruction
        // Doorway connects (7, 15) to (7, 16)
        final boolean[] raycastBlocked = {false};
        world.rayCast((fixture, point, normal, fraction) -> {
            if ((fixture.getFilterData().categoryBits & PhysicsConstants.CATEGORY_WALL) != 0) {
                raycastBlocked[0] = true;
                return 0f; // Terminate raycast
            }
            return 1f;
        }, new Vector2(7.5f, 14.5f), new Vector2(7.5f, 17.5f));

        assertTrue(raycastBlocked[0], "Raycast through closed doorway must be blocked by gate barrier");

        // 3. Simulate room wipe (all enemies defeated)
        assertTrue(combatRoom.activeEnemies.size > 0, "Lockdown chamber must have active enemies spawned");
        for (int i = 0; i < combatRoom.activeEnemies.size; i++) {
            Entity enemy = combatRoom.activeEnemies.get(i);
            HealthComponent health = ComponentMappers.health.get(enemy);
            if (health != null) {
                health.currentHp = 0f; // Kill enemy
            }
        }

        // Update manager to detect wipe
        lockdownManager.update(0.016f, combatRoom.gridX + 8f, combatRoom.gridY + 8f);

        // 4. Verify lockdown lifted and bodies removed
        assertFalse(lockdownManager.isLockdownActive(), "Lockdown must be lifted after room wipe");
        assertEquals(PlacedRoom.RoomState.CLEARED, combatRoom.state);
        assertEquals(0, combatRoom.gateBodies.size, "All gate bodies must be destroyed after room wipe");

        // Raycast again through doorway: must be completely unblocked!
        raycastBlocked[0] = false;
        world.rayCast((fixture, point, normal, fraction) -> {
            if ((fixture.getFilterData().categoryBits & PhysicsConstants.CATEGORY_WALL) != 0) {
                raycastBlocked[0] = true;
                return 0f;
            }
            return 1f;
        }, new Vector2(7.5f, 14.5f), new Vector2(7.5f, 17.5f));

        assertFalse(raycastBlocked[0], "Raycast through opened doorway must pass freely without obstruction");

        lockdownManager.dispose();
        world.dispose();
    }
}
