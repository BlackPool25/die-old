# TDD-01: Technical Stack & Software Architecture

## 1. Technical Stack Overview

| Layer | Technology | Version | Rationale |
| :--- | :--- | :--- | :--- |
| **Language** | Java | **OpenJDK 21 (LTS)** | Records for telemetry data, pattern matching for ECS systems, virtual threads for non-blocking procedural generation. |
| **Game Engine** | **LibGDX** | **1.13.x** | Industry battle-tested 2D/2.5D framework (*Slay the Spire*, *Mindustry*). Native OpenGL desktop backend with LWJGL3. |
| **Architecture** | **Ashley ECS** | **1.7.x** | High-performance Entity Component System. Clean decoupling of physics, AI, rendering, and gameplay logic. |
| **Physics** | **Box2D (via LibGDX)** | Native C++ bindings | High-precision 60 FPS rigid-body collisions, raycasting, dynamic friction, and restitution. |
| **Map Authoring** | **Tiled Map Editor** | **1.11.x (.tmx / JSON)** | Standardized layer authoring for modular prefab structures, loaded via LibGDX's `TmxMapLoader`. |
| **Build Tool** | **Gradle** | **8.x** | Multi-module structure (`core`, `desktop`, `assets`, `tools`). Simple dependency resolution and packaging. |

---

## 2. Directory & Package Layout

```
game-fun/
├── build.gradle                   # Root Gradle build script
├── settings.gradle                # Multi-project inclusions
├── gradle/wrapper/                # Gradle wrapper binaries
├── assets/                        # Shared runtime game assets
│   ├── maps/prefabs/              # Tiled .tmx modular room templates
│   ├── sprites/                   # Texture atlases (dimetric 2.5D sprites)
│   ├── shaders/                   # GLSL shaders (dimetric lighting, shadows)
│   └── audio/                     # Sound effects, adaptive music stems
│
├── core/src/main/java/com/dieold/game/
│   ├── DieOldGame.java            # LibGDX Game entrypoint & screen manager
│   ├── ecs/
│   │   ├── components/            # Data-only components (Position, Velocity, Poise, etc.)
│   │   ├── systems/               # Logic-only systems (Movement, Combat, AI, Render)
│   │   └── EntityFactory.java     # Entity instantiation helper
│   ├── ai/
│   │   ├── blackboard/            # NeuralBlackboard singleton & metric trackers
│   │   ├── utility/               # Utility consideration curves & decision nodes
│   │   └── behaviors/             # Enemy behavior actions (Flank, Parry, FlakBurst)
│   ├── procedural/
│   │   ├── JigsawGenerator.java   # Core socket-stitching generation engine
│   │   ├── RoomTemplate.java      # Model for authored Tiled prefabs
│   │   ├── SocketConnector.java   # Socket alignment & bounding-box logic
│   │   └── ConnectivitySolver.java# A* path reachability validator
│   ├── physics/
│   │   ├── PhysicsWorld.java      # Box2D world wrapper & collision categories
│   │   └── ContactListener.java   # Collision dispatcher for wall-slams & deflects
│   ├── model/
│   │   ├── AgeStage.java          # Enum representing biological life stages
│   │   ├── Rune.java              # Rune records (Trigger, Form, Element)
│   │   └── TelemetrySnapshot.java # Immutable 60-second player metric record
│   └── screens/
│       ├── GameScreen.java        # Main active gameplay viewport & loop
│       ├── SanctuaryScreen.java   # Meta-progression hub screen
│       └── ChronoRevivalScreen.java# Aging / Amnesia Purge decision UI
│
├── desktop/                       # LWJGL3 Desktop launcher & configuration
└── docs/                          # Pre-production specification files (GDD/TDD/ADR/QA)
```

---

## 3. Core Game Loop & Frame Budget (16.6ms Target)

To prevent Garbage Collection (GC) pauses and ensure a rock-solid 60 FPS:
1. **Zero Allocations in System Updates:** No `new` allocations allowed inside `update(float deltaTime)` loops. Vectors, iterators, and temporary math objects are pre-allocated or pooled via LibGDX `Pool<T>`.
2. **Fixed Physics Timestep:** Box2D steps at a deterministic `1/60f` using an accumulator to guarantee stable collision checks regardless of variable rendering delta times.
3. **Decoupled Background Level Generation:** Jigsaw room layout assembly, socket matching, and A* pathfinding validation execute asynchronously on Java 21 Virtual Threads (`Thread.ofVirtual().start(...)`) operating strictly on data records (`DungeonBlueprint`). Once validated, GPU texture uploads and mesh instantiations are scheduled on the main render thread via `Gdx.app.postRunnable()`, preventing OpenGL context crashes while eliminating load hitches.
