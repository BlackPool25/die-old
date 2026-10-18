package com.dieold.game;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.dieold.game.render.IsometricProjection;
import com.dieold.game.world.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless test validating ChamberCameraSystem:
 * 1. Screen diamond bounding box calculation for various room dimensions (14x14, 20x20, 10x20).
 * 2. Camera clamping math strictly within room boundaries without revealing void.
 * 3. Smooth bounds interpolation during room-to-room transitions.
 */
public class ChamberCameraSystemTest {

    @BeforeAll
    public static void initNatives() {
        GdxNativesLoader.load();
    }

    @Test
    @DisplayName("Verify dimetric screen bounding box calculations for various room sizes")
    public void testRoomScreenBoundsCalculations() {
        RoomTemplate t14 = new RoomTemplate("Small14", 14, 14, true);
        RoomTemplate t20 = new RoomTemplate("Medium20", 20, 20, true);
        RoomTemplate tConduit = new RoomTemplate("Conduit", 10, 20, false);

        PlacedRoom room14 = new PlacedRoom(t14, 0, 0);
        PlacedRoom room20 = new PlacedRoom(t20, 10, -5);
        PlacedRoom roomConduit = new PlacedRoom(tConduit, -15, 20);

        // Verify bounds are non-empty and properly ordered
        assertTrue(room14.maxScreenX > room14.minScreenX);
        assertTrue(room14.maxScreenY > room14.minScreenY);

        assertTrue(room20.maxScreenX > room20.minScreenX);
        assertTrue(room20.maxScreenY > room20.minScreenY);

        assertTrue(roomConduit.maxScreenX > roomConduit.minScreenX);
        assertTrue(roomConduit.maxScreenY > roomConduit.minScreenY);

        // For a square room (14x14) at (0, 0):
        // South corner: (0, 0) -> Screen: (0, 0)
        // East corner: (14, 0) -> Screen: (14 * 27.7128, 14 * 16) = (~387.98, 224)
        // West corner: (0, 14) -> Screen: (-14 * 27.7128, 14 * 16) = (~-387.98, 224)
        // North corner: (14, 14) -> Screen: (0, 28 * 16) = (0, 448)
        float expectedMinX = -14f * IsometricProjection.X_STEP;
        float expectedMaxX = 14f * IsometricProjection.X_STEP;
        float expectedMinY = 0f;
        float expectedMaxY = 28f * IsometricProjection.Y_STEP;

        assertEquals(expectedMinX, room14.minScreenX, 1e-2f, "Small14 minScreenX calculation");
        assertEquals(expectedMaxX, room14.maxScreenX, 1e-2f, "Small14 maxScreenX calculation");
        assertEquals(expectedMinY, room14.minScreenY, 1e-2f, "Small14 minScreenY calculation");
        assertEquals(expectedMaxY, room14.maxScreenY, 1e-2f, "Small14 maxScreenY calculation");
    }

    @Test
    @DisplayName("Verify camera clamping math strictly within room boundaries")
    public void testCameraClampingMath() {
        float viewportW = 640f;
        float viewportH = 360f;
        float halfW = viewportW * 0.5f;
        float halfH = viewportH * 0.5f;

        // Big room: screen bounds from -600 to +600 in X, 0 to 600 in Y
        float minX = -600f;
        float maxX = 600f;
        float minY = 0f;
        float maxY = 600f;

        // 1. Target at center (0, 300): Camera should be exactly at center
        Vector2 camCenter = ChamberCameraSystem.calculateClampedCamera(
            0f, 300f, minX, maxX, minY, maxY, viewportW, viewportH
        );
        assertEquals(0f, camCenter.x, 1e-2f);
        assertEquals(300f, camCenter.y, 1e-2f);

        // 2. Target far left (-1000, 300): Camera must clamp at minX + halfW
        Vector2 camLeft = ChamberCameraSystem.calculateClampedCamera(
            -1000f, 300f, minX, maxX, minY, maxY, viewportW, viewportH
        );
        assertEquals(minX + halfW, camLeft.x, 1e-2f, "Camera must clamp left edge");
        assertEquals(300f, camLeft.y, 1e-2f);

        // 3. Target far right (+1000, 300): Camera must clamp at maxX - halfW
        Vector2 camRight = ChamberCameraSystem.calculateClampedCamera(
            1000f, 300f, minX, maxX, minY, maxY, viewportW, viewportH
        );
        assertEquals(maxX - halfW, camRight.x, 1e-2f, "Camera must clamp right edge");
        assertEquals(300f, camRight.y, 1e-2f);

        // 4. Target far bottom (0, -500): Camera must clamp at minY + halfH
        Vector2 camBottom = ChamberCameraSystem.calculateClampedCamera(
            0f, -500f, minX, maxX, minY, maxY, viewportW, viewportH
        );
        assertEquals(0f, camBottom.x, 1e-2f);
        assertEquals(minY + halfH, camBottom.y, 1e-2f, "Camera must clamp bottom edge");

        // 5. Target far top (0, 1000): Camera must clamp at maxY - halfH
        Vector2 camTop = ChamberCameraSystem.calculateClampedCamera(
            0f, 1000f, minX, maxX, minY, maxY, viewportW, viewportH
        );
        assertEquals(0f, camTop.x, 1e-2f);
        assertEquals(maxY - halfH, camTop.y, 1e-2f, "Camera must clamp top edge");
    }

    @Test
    @DisplayName("Verify smooth camera bounds interpolation during room-to-room transitions")
    public void testCameraBoundsInterpolation() {
        OrthographicCamera cam = new OrthographicCamera(640, 360);
        DungeonLevel level = new DungeonLevel();

        RoomTemplate t1 = new RoomTemplate("Room1", 16, 16, false);
        RoomTemplate t2 = new RoomTemplate("Room2", 16, 16, false);

        PlacedRoom r1 = new PlacedRoom(t1, 0, 0);
        PlacedRoom r2 = new PlacedRoom(t2, 16, 0);

        level.getRooms().add(r1);
        level.getRooms().add(r2);

        ChamberCameraSystem cameraSystem = new ChamberCameraSystem(cam, level);
        cameraSystem.setRoom(r1, true);

        assertEquals(r1.minScreenX, cameraSystem.currentMinX, 1e-2f);
        assertEquals(r1.maxScreenX, cameraSystem.currentMaxX, 1e-2f);

        // Trigger transition to Room2
        cameraSystem.setRoom(r2, false);

        // Initial current bounds are still at Room1
        assertEquals(r1.minScreenX, cameraSystem.currentMinX, 1e-2f);
        // Target bounds are at Room2
        assertEquals(r2.minScreenX, cameraSystem.targetMinX, 1e-2f);

        // Advance 0.1s
        cameraSystem.update(0.1f, 18.0f, 8.0f, 0f);

        // Current bounds must have smoothly moved towards Room2
        assertNotEquals(r1.minScreenX, cameraSystem.currentMinX);
        assertTrue(cameraSystem.currentMinX > r1.minScreenX, "MinScreenX should interpolate towards Room2");
    }
}
