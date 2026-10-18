# ADR-003: Top-Down Box2D Physics with Simulated Z-Axis vs Custom AABB

## Status
**Accepted**

## Context
In a 2.5D isometric game with high-speed dashes, multi-enemy knockback, wall-slam impacts, and deflection physics, we need a physics system that guarantees:
1. No tunneling or phase-clipping through thin walls at high velocities.
2. Continuous collision detection (CCD) for fast-moving deflected projectiles.
3. Realistic momentum transfer, friction, and restitution (bouncing off walls/pillars).
4. Support for height / verticality (jumping over low ground hazards, falling off cliff ledges).

### Evaluated Alternatives
1. **Custom 2D AABB (Axis-Aligned Bounding Box) Physics:**
   - *Pros:* Simple to write initially.
   - *Cons:* Terrible at handling angled isometric collisions, rotating hitboxes, non-axis-aligned momentum transfer, and fast-moving projectile tunneling without writing hundreds of lines of complex solver math.
2. **Full 3D Physics Engine (Bullet / PhysX):**
   - *Pros:* Native 3D coordinates.
   - *Cons:* Massive overhead, complex collision meshes, and overkill for a game whose core movement plane is a 2D isometric surface.
3. **Top-Down 2D Box2D with Simulated Z-Axis:**
   - *Architecture:*
     - The ground plane $(X, Y)$ is managed directly by a standard Box2D `World` with gravity set to $(0, 0)$.
     - Height ($Z$-axis) is managed as a scalar float in `TransformComponent`.
     - When entities jump or get knocked upward, $z > 0$. Collisions with low ground hazards (spikes, acid pools) are conditionally ignored if $z > \text{hazardHeight}$.
     - Shadows are projected on the ground plane at $(x, y)$, scaling dynamically with height $z$.

## Decision
We adopt **Top-Down Box2D with a Simulated Z-Axis**.

## Consequences
- Rock-solid collision detection with zero tunneling, backed by Box2D's native C++ solver.
- Natural wall-slam impact detection by reading `ContactImpulse` directly in the Box2D `ContactListener`.
- Clean visual depth: jumping, knockback lofting, and ledge drops feel tactile without 3D engine bloat.
