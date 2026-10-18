# ADR-002: Entity-Component-System (Ashley ECS) vs Object-Oriented Hierarchy

## Status
**Accepted**

## Context
Video games often suffer from the "Dreaded Diamond of Inheritance" problem in traditional Object-Oriented Programming (OOP). For instance, if an entity is both a `DestructiblePillar`, an `ElementalConductor`, and an `AIBlackboardListener`, an OOP hierarchy forces multiple inheritance conflicts or deeply bloated `GameObject` God-classes.

Furthermore, dynamic gameplay features like our **Aging System** (which changes socket capacity, health, and mobility) and **Adaptive AI** (which dynamically attaches shields or flak cannons to enemies) require modifying an entity's capabilities at runtime.

### Evaluated Alternatives
1. **Traditional OOP Inheritance (`abstract class Entity` -> `Monster` -> `Boss`):**
   - *Failure Mode:* Inflexible. If an enemy learns how to use a shield or player dash, changing its class hierarchy at runtime is impossible without messy boolean flags and code bloat.
2. **Component-Based OOP (Unity-style `GameObject.addComponent()`):**
   - *Failure Mode:* Logic and data remain tangled within individual component classes.
3. **Data-Oriented Entity Component System (Ashley ECS):**
   - *Pros:* Complete separation of pure data (`Component`) from pure behavior (`EntitySystem`). Entities are simply lightweight integer IDs. Components can be added or removed dynamically in a single line of code (`entity.add(new ShieldComponent())`).

## Decision
We adopt **Ashley ECS** as the structural backbone for all runtime entities, systems, and listeners in *Chronos-Echo*.

## Consequences
- High modularity: New mechanics (e.g. ice freezing, aging penalties, deflection pulses) are isolated to distinct systems without touching existing code.
- Predictable execution order: Systems run in strict priority sequence per frame.
- High performance: Iterating over compact component arrays optimizes CPU cache locality and eliminates virtual method dispatch overhead.
