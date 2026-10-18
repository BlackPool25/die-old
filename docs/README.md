# Die Old
## Master Pre-Production & Engineering Suite

> **Title:** *Die Old*  
> **Tagline:** *"Every death costs a decade. Escape before 80."*  
> **Genre:** 2.5D Isometric Action Roguelite with Adaptive AI & Generational Aging  
> **Tech Stack:** Java 21, LibGDX 1.13.x, Ashley ECS, Box2D, Tiled Map Editor  
> **Status:** Phase 0 (Production-Locked Engineering Specification)

---

## Document Index & Traceability Matrix

### 1. Game Design Documents (`docs/GDD/`)
Comprehensive game systems, player experience goals, rules, and mathematical balances:
- [01_executive_summary.md](file:///home/shreyas/projects/game-fun/docs/GDD/01_executive_summary.md) — High concept, pillars, player fantasy, and core loop.
- [02_combat_and_mechanics.md](file:///home/shreyas/projects/game-fun/docs/GDD/02_combat_and_mechanics.md) — 2.5D isometric combat, 360° Deflection pulse, crowd control, stamina/posture, and wall-slam physics.
- [03_neural_ai_adaptation.md](file:///home/shreyas/projects/game-fun/docs/GDD/03_neural_ai_adaptation.md) — Shared Neural Blackboard, telemetry tracking, counter-evolution triggers, anti-OP balancing.
- [04_aging_and_samsara.md](file:///home/shreyas/projects/game-fun/docs/GDD/04_aging_and_samsara.md) — Sifu-style aging curve, life stages, death dilemmas, and Neural Purge difficulty resets.
- [05_procedural_structures.md](file:///home/shreyas/projects/game-fun/docs/GDD/05_procedural_structures.md) — Minecraft-style jigsaw prefab sockets, Tiled layer rules, age-gated secrets.
- [06_progression_and_economy.md](file:///home/shreyas/projects/game-fun/docs/GDD/06_progression_and_economy.md) — In-run rune sockets, Sanctuary archive, bloodline inheritances.
- [07_boss_encounters_and_adaptation.md](file:///home/shreyas/projects/game-fun/docs/GDD/07_boss_encounters_and_adaptation.md) — Multi-phase bosses, real-time in-fight learning, generational memory, and execution finishers.
- [08_art_and_visual_direction.md](file:///home/shreyas/projects/game-fun/docs/GDD/08_art_and_visual_direction.md) — Dark Industrial Alchemical Gothic aesthetic, strict 2:1 dimetric standards, 3D-to-2D Blender baking pipeline, and anti-slop rules.

### 2. Technical Design Documents (`docs/TDD/`)
Engineering architecture, memory layout, algorithms, and system specifications:
- [01_tech_stack_and_architecture.md](file:///home/shreyas/projects/game-fun/docs/TDD/01_tech_stack_and_architecture.md) — Java 21, Gradle multi-module layout, LibGDX subsystems, directory tree.
- [02_ecs_specification.md](file:///home/shreyas/projects/game-fun/docs/TDD/02_ecs_specification.md) — Complete Ashley Entity-Component-System schemas and execution order.
- [03_ai_utility_engine.md](file:///home/shreyas/projects/game-fun/docs/TDD/03_ai_utility_engine.md) — Utility AI scoring math, Blackboard data model, behavior evaluation loops.
- [04_level_generator_pipeline.md](file:///home/shreyas/projects/game-fun/docs/TDD/04_level_generator_pipeline.md) — Jigsaw socket-matching algorithm, bounding box collision, A* connectivity solver.
- [05_rendering_and_art_pipeline.md](file:///home/shreyas/projects/game-fun/docs/TDD/05_rendering_and_art_pipeline.md) — 2.5D dimetric rendering, Y-sorting, dynamic Box2D-Lights, clean asset standards (No AI-slop).

### 3. Architecture Decision Records (`docs/ADR/`)
Context, evaluated alternatives, and rationale for foundational technical choices:
- [ADR-001-engine-selection.md](file:///home/shreyas/projects/game-fun/docs/ADR/ADR-001-engine-selection.md) — Selection of LibGDX over LWJGL/FXGL/Raylib.
- [ADR-002-ecs-architecture.md](file:///home/shreyas/projects/game-fun/docs/ADR/ADR-002-ecs-architecture.md) — Ashley ECS vs Object-Oriented inheritance hierarchies.
- [ADR-003-physics-model.md](file:///home/shreyas/projects/game-fun/docs/ADR/ADR-003-physics-model.md) — Top-down Box2D with simulated Z-axis vs custom AABB.

### 4. Quality Assurance & Benchmarks (`docs/QA/`)
Verification criteria, headless regression test harness, and performance budgets:
- [test_plan_and_benchmarks.md](file:///home/shreyas/projects/game-fun/docs/QA/test_plan_and_benchmarks.md) — 10,000-seed headless dungeon generation validation, 60 FPS / 16ms frame-time budget, zero-allocation game loop.
