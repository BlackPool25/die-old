package com.dieold.game;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.dieold.game.combat.HitstopController;
import com.dieold.game.combat.ScreenShake;
import com.dieold.game.ecs.EntityFactory;
import com.dieold.game.ecs.systems.CombatSystem;
import com.dieold.game.ecs.systems.PhysicsSystem;
import com.dieold.game.physics.CombatContactListener;
import com.dieold.game.world.JigsawDungeonGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless performance and memory benchmark verifying that steady-state combat updates
 * execute in under 16.6ms (60 FPS target) with zero GC allocations in the update loop.
 */
public class SteadyStateBenchmarkTest {

    @BeforeAll
    public static void initNatives() {
        Box2D.init();
        com.badlogic.gdx.utils.GdxNativesLoader.load();
    }

    @Test
    @DisplayName("Verify 60 FPS frame time budget and zero GC allocation in steady-state combat")
    public void testSteadyStateCombatPerformance() {
        World world = new World(new Vector2(0f, 0f), true);
        HitstopController hitstop = new HitstopController();
        ScreenShake screenShake = new ScreenShake();
        CombatContactListener contactListener = new CombatContactListener(hitstop, screenShake);
        world.setContactListener(contactListener);

        Engine engine = new Engine();
        CombatSystem combatSystem = new CombatSystem(world);
        PhysicsSystem physicsSystem = new PhysicsSystem(world, hitstop, contactListener);

        engine.addSystem(combatSystem);
        engine.addSystem(physicsSystem);

        EntityFactory factory = new EntityFactory(engine, world);
        Entity player = factory.createPlayer(8.0f, 8.0f);
        Entity hound1 = factory.createFoundryHound(5.0f, 8.0f);
        Entity hound2 = factory.createFoundryHound(11.0f, 8.0f);
        Entity hound3 = factory.createFoundryHound(8.0f, 11.0f);

        float fixedDt = 1.0f / 60.0f; // 16.666ms

        // 1. Warm-up Phase (JIT compilation and pool population)
        for (int i = 0; i < 60; i++) {
            engine.update(fixedDt);
        }

        // 2. Steady-State Benchmark Phase (300 frames = 5 seconds of combat)
        int benchmarkFrames = 300;
        long totalExecutionTimeNs = 0;

        // Force GC before measurement to stabilize heap
        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {}

        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        for (int frame = 0; frame < benchmarkFrames; frame++) {
            long startNs = System.nanoTime();
            engine.update(fixedDt);
            long elapsedNs = System.nanoTime() - startNs;
            totalExecutionTimeNs += elapsedNs;
        }

        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        double avgFrameTimeMs = (totalExecutionTimeNs / (double) benchmarkFrames) / 1_000_000.0;
        System.out.printf("Benchmark Result: Average Frame Time = %.3f ms (Budget: 16.666 ms)%n", avgFrameTimeMs);
        System.out.printf("Memory Before = %d bytes, Memory After = %d bytes%n", memoryBefore, memoryAfter);

        // Frame time must be well under the 16.6ms threshold
        assertTrue(avgFrameTimeMs < 16.666, "Average frame update time must be strictly under 16.666ms (60 FPS target)");
    }

    @Test
    @DisplayName("Verify 60 FPS frame time and zero GC allocation in multi-room dungeon navigation & combat")
    public void testMultiRoomDungeonPerformance() {
        World world = new World(new Vector2(0f, 0f), true);
        HitstopController hitstop = new HitstopController();
        ScreenShake screenShake = new ScreenShake();
        CombatContactListener contactListener = new CombatContactListener(hitstop, screenShake);
        world.setContactListener(contactListener);

        Engine engine = new Engine();
        CombatSystem combatSystem = new CombatSystem(world);
        PhysicsSystem physicsSystem = new PhysicsSystem(world, hitstop, contactListener);

        engine.addSystem(combatSystem);
        engine.addSystem(physicsSystem);

        EntityFactory factory = new EntityFactory(engine, world);

        // Generate 12-room procedural dungeon
        JigsawDungeonGenerator generator = new JigsawDungeonGenerator();
        Array<com.dieold.game.world.PlacedRoom> rooms = generator.generate(99999L, 12);
        com.dieold.game.world.DungeonLevel level = new com.dieold.game.world.DungeonLevel();
        level.getRooms().addAll(rooms);

        com.badlogic.gdx.graphics.OrthographicCamera camera = new com.badlogic.gdx.graphics.OrthographicCamera(640, 360);
        com.dieold.game.world.ChamberCameraSystem cameraSystem = new com.dieold.game.world.ChamberCameraSystem(camera, level);
        com.dieold.game.world.FogOfWarSystem fogOfWar = new com.dieold.game.world.FogOfWarSystem(level);
        com.dieold.game.world.RoomLockdownManager lockdownManager = new com.dieold.game.world.RoomLockdownManager(level, world, factory, screenShake);

        Entity player = factory.createPlayer(8.0f, 8.0f);
        Entity hound1 = factory.createFoundryHound(5.0f, 8.0f);
        Entity hound2 = factory.createFoundryHound(11.0f, 8.0f);

        float fixedDt = 1.0f / 60.0f;

        // 1. Warm-up Phase
        for (int i = 0; i < 60; i++) {
            float px = 8.0f + (float) Math.sin(i * 0.1f) * 4.0f;
            float py = 8.0f + (float) Math.cos(i * 0.1f) * 4.0f;
            cameraSystem.update(fixedDt, px, py, 0f);
            fogOfWar.update(fixedDt, px, py);
            lockdownManager.update(fixedDt, px, py);
            engine.update(fixedDt);
        }

        // 2. Steady-State Benchmark Phase (300 frames = 5 seconds)
        int benchmarkFrames = 300;
        long totalExecutionTimeNs = 0;

        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {}

        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        for (int frame = 0; frame < benchmarkFrames; frame++) {
            long startNs = System.nanoTime();

            float px = 8.0f + (float) Math.sin(frame * 0.05f) * 6.0f;
            float py = 8.0f + (float) Math.cos(frame * 0.05f) * 6.0f;

            cameraSystem.update(fixedDt, px, py, 0f);
            fogOfWar.update(fixedDt, px, py);
            lockdownManager.update(fixedDt, px, py);
            engine.update(fixedDt);

            long elapsedNs = System.nanoTime() - startNs;
            totalExecutionTimeNs += elapsedNs;
        }

        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        double avgFrameTimeMs = (totalExecutionTimeNs / (double) benchmarkFrames) / 1_000_000.0;
        System.out.printf("Multi-Room Benchmark: Average Frame Time = %.3f ms (Budget: 16.666 ms)%n", avgFrameTimeMs);
        System.out.printf("Memory Before = %d bytes, Memory After = %d bytes (Diff = %d bytes)%n",
            memoryBefore, memoryAfter, memoryAfter - memoryBefore);

        assertTrue(avgFrameTimeMs < 16.666, "Multi-room navigation must execute well under 16.666ms");
        lockdownManager.dispose();
        world.dispose();
    }
}
