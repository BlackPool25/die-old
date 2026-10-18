# ADR-001: Selection of LibGDX as the Core Game Engine

## Status
**Accepted**

## Context
We need a robust, high-performance Java framework capable of supporting:
1. 2.5D Isometric rendering with dynamic $Y$-sorting and custom shaders.
2. High-performance physics simulation with native C++ rigid body bindings.
3. Modular Tiled (`.tmx`) map loading for procedural structure stitching.
4. Deterministic cross-platform desktop execution (Linux, Windows, macOS) with single-binary or native JPackage distribution.

### Evaluated Alternatives
1. **LWJGL 3 (Bare Metal):**
   - *Pros:* Ultimate control over OpenGL/Vulkan pipelines (used by *Minecraft*).
   - *Cons:* Requires writing sprite batchers, font renderers, texture packers, input wrappers, and matrix math libraries from scratch. Massively inflates pre-production time without gameplay benefit.
2. **FXGL (JavaFX Game Engine):**
   - *Pros:* Clean modern Java API, built-in ECS.
   - *Cons:* Bound to JavaFX rendering pipeline; lacks raw OpenGL shader control, has higher garbage collection overhead under heavy particle/bullet loads, and lacks mature Box2D integration.
3. **Raylib-J (Jaylib):**
   - *Pros:* Extremely lightweight, clean immediate-mode C-bindings.
   - *Cons:* Lacks high-level scene management, Tiled map loaders, and mature asset packing pipelines.
4. **LibGDX:**
   - *Pros:* Battle-tested by major commercial indie successes (*Slay the Spire*, *Mindustry*, *Pathway*). Built-in Box2D physics, Ashley ECS, native Tiled `.tmx` parser, mature OpenGL batching, and active community.
   - *Cons:* Project setup requires Gradle multi-module structure (well-understood and standard).

## Decision
We select **LibGDX 1.13.x with the LWJGL3 Desktop Backend** running on **Java 21 LTS**.

## Consequences
- Fast development velocity with battle-tested subsystems (Box2D, SpriteBatch, TiledMapLoader).
- Full access to modern Java 21 features (Virtual Threads, Records, Pattern Matching).
- Direct path to native desktop distribution without JVM installation requirements using `jpackage`.
