# TDD-02: Entity Component System (Ashley ECS) Specification

## 1. Architectural Rules for ECS
1. **Components are Pure Data:** Components must contain public fields or getters/setters with zero game logic, methods, or internal state manipulation.
2. **Systems are Pure Logic:** Systems process sets of entities filtered by Ashley `Family` matchers.
3. **Deterministic System Priority:** Every system is assigned an explicit integer priority to guarantee execution order per frame.

---

## 2. Complete Component Catalog

```mermaid
classDiagram
    class TransformComponent {
        +Vector3 position (x, y, z)
        +Vector2 velocity
        +float rotationDegrees
        +float scale
    }
    class PhysicsBodyComponent {
        +Body box2dBody
        +short categoryBits
        +short maskBits
    }
    class HealthComponent {
        +float currentHp
        +float maxHp
        +float poise
        +float maxPoise
        +boolean isVulnerable
    }
    class AgingComponent {
        +int currentAge
        +AgeStage stage
        +int deathCount
        +int socketCapacity
    }
    class DeflectionComponent {
        +float activeTimer
        +float windowDuration (0.12s)
        +float radius
        +boolean isDeflecting
    }
    class RunicMatrixComponent {
        +List~Rune~ socketedRunes
        +float cooldownRemaining
    }
    class TelemetrySubjectComponent {
        +RollingMetrics metrics
    }
    class UtilityAIComponent {
        +AIArchetype archetype
        +float thinkTimer
        +AIAction currentAction
    }
```

---

## 3. System Execution Order & Priorities

Each frame, the Ashley `Engine.update(deltaTime)` executes systems in this strict sequence:

```mermaid
flowchart TD
    S1["1. InputSystem\n(Reads Keyboard/Gamepad & sets Intent/Velocity)"] --> S2["2. TelemetrySystem\n(Samples player actions into Neural Blackboard)"]
    S2 --> S3["3. UtilityAISystem\n(Evaluates enemy considerations & sets AI goals)"]
    S3 --> S4["4. CombatSystem\n(Handles attack hitboxes, deflection windows, combos)"]
    S4 --> S5["5. PhysicsSystem\n(Steps Box2D world & syncs TransformComponent coordinates)"]
    S5 --> S6["6. CollisionResponseSystem\n(Processes wall-slam impacts & hazard triggers)"]
    S6 --> S7["7. AgingSystem\n(Monitors lethal damage & triggers Chrono-Revival UI)"]
    S7 --> S8["8. YSortRenderSystem\n(Calculates dimetric projection & draws depth-sorted sprites)"]
    S8 --> S9["9. LightingSystem\n(Renders Box2D-Lights & dynamic normal-mapped shadows)"]
```

### System Priority Table:

| Priority | System Name | Ashley Family Filter | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| **10** | `InputSystem` | `PlayerComponent, TransformComponent` | Translates raw hardware input into movement and attack vectors. |
| **20** | `TelemetrySystem` | `PlayerComponent, TelemetrySubjectComponent` | Aggregates 60-second rolling combat data into the shared `Blackboard`. |
| **30** | `UtilityAISystem` | `UtilityAIComponent, PhysicsBodyComponent` | Re-evaluates behavior scores based on player threat profiles. |
| **40** | `CombatSystem` | `CombatComponent, HealthComponent` | Handles active frames, damage resolution, and poise breaks. |
| **50** | `PhysicsSystem` | `PhysicsBodyComponent, TransformComponent` | Runs Box2D accumulator step and updates entity transforms. |
| **60** | `WallSlamSystem` | `PhysicsBodyComponent, HealthComponent` | Resolves momentum loss against walls into crushing damage. |
| **70** | `AgingSystem` | `PlayerComponent, AgingComponent` | Intercepts zero-HP events and initiates the Samsara lifecycle. |
| **80** | `YSortRenderSystem` | `TransformComponent, TextureComponent` | Sorts entities by base $Y$-value and renders via `SpriteBatch`. |
| **90** | `LightingSystem` | All Light / Shadow entities | Draws dynamic ambient occlusion, shadows, and point lights. |
