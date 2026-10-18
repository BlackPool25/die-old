package com.dieold.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.dieold.game.world.DungeonReachabilityValidator;
import com.dieold.game.world.JigsawDungeonGenerator;
import com.dieold.game.world.PlacedRoom;
import com.dieold.game.world.PlacedSocket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless procedural world generation verification suite:
 * Executes 100 random seeded generations and asserts:
 * 1. 0 room overlaps across all placed rooms.
 * 2. 100% A* reachability from IngressHub spawn to all room centers.
 * 3. 0 uncapped open sockets.
 */
public class ProceduralGenerationTest {

    @Test
    @DisplayName("Verify 100 random seeded generations: 0 overlaps, 100% reachability, 0 uncapped sockets")
    public void test100SeededGenerations() {
        JigsawDungeonGenerator generator = new JigsawDungeonGenerator();
        DungeonReachabilityValidator validator = new DungeonReachabilityValidator();

        int testSeeds = 100;
        int targetRooms = 12;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < testSeeds; i++) {
            long seed = 1000L + i * 31L;
            Array<PlacedRoom> rooms = generator.generate(seed, targetRooms);

            assertNotNull(rooms, "Generated room layout should not be null for seed " + seed);
            assertTrue(rooms.size >= 8, "Expected at least 8 rooms, got " + rooms.size + " for seed " + seed);

            // 1. Assert 0 room overlaps
            for (int a = 0; a < rooms.size; a++) {
                Rectangle boundsA = rooms.get(a).bounds;
                for (int b = a + 1; b < rooms.size; b++) {
                    Rectangle boundsB = rooms.get(b).bounds;
                    assertFalse(boundsA.overlaps(boundsB),
                        "Room overlap detected between room " + a + " and room " + b + " for seed " + seed);
                }
            }

            // 2. Assert 0 uncapped open sockets
            for (int r = 0; r < rooms.size; r++) {
                PlacedRoom room = rooms.get(r);
                for (int s = 0; s < room.sockets.size; s++) {
                    PlacedSocket socket = room.sockets.get(s);
                    assertTrue(socket.isConnected() || socket.isCapped,
                        "Uncapped open socket found in room " + room.template.id + " for seed " + seed);
                }
            }

            // 3. Assert 100% A* reachability
            PlacedRoom ingress = rooms.get(0);
            int spawnX = ingress.gridX + 8;
            int spawnY = ingress.gridY + 8;
            boolean reachable = validator.validate(rooms, spawnX, spawnY);
            assertTrue(reachable, "Dungeon failed A* reachability check for seed " + seed);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("Successfully verified %d procedural generations in %d ms (avg %.2f ms/generation)%n",
            testSeeds, elapsed, (double) elapsed / testSeeds);
    }
}
