# GDD-01: Executive Summary & High Concept

## 1. High Concept
**Die Old** is a high-octane 2.5D isometric action roguelite where every death costs a decade of your life. Players start at Age 20 navigating an adaptive subterranean megastructure governed by an evolving AI hive mind that tracks your combat habits and mutates to counter them. Each time you fall, you age by 10 years—sacrificing physical resilience for reality-altering eldritch spell mastery. Can you break the cycle and escape before you reach Age 80 and die of old age?

## 2. Core Pillars

| Pillar | Design Goal | Player Feeling |
| :--- | :--- | :--- |
| **I. The Mirror Hive** | Dynamic counter-adaptation driven by a shared AI blackboard. Spamming one weapon or move creates lethal counter-units. | *"The dungeon is studying me. I need to switch my style."* |
| **II. The Samsara Cycle** | Aging upon death (*Sifu*-inspired). Youth provides agility and health; old age provides massive runic sockets and wisdom, but fragility. | *"Do I risk my youth to stay strong, or age to reset their lethal counters?"* |
| **III. Kinetic 2.5D Mastery** | Fast, responsive isometric combat with 360° deflection, wall-slams, and multi-enemy crowd control. | *"I am orchestrating chaos, deflecting projectile storms and smashing foes into pillars."* |
| **IV. Modular Jigsaw Worlds** | Minecraft-style socket-based structure generation using Tiled `.tmx` templates with secrets unlocked by age-gated wisdom. | *"Every sector feels hand-crafted and cohesive, yet entirely unpredictable."* |

## 3. Target Audience & Comparable Titles
- **Primary Audience:** Fans of fast-paced, high-mastery action roguelites (*Hades*, *Dead Cells*, *Curse of the Dead Gods*, *Sifu*).
- **Unique Selling Proposition (USP):**
  1. **Anti-OP Adaptive AI:** Eliminates the "broken build boredom" of traditional roguelikes.
  2. **Age-Driven Stat Morphing:** Player attributes don't just go up; they transform in trade-offs (Health $\leftrightarrow$ Spell Capacity).
  3. **Architectural Jigsaw Assembly:** Levels feel structurally authentic, complete with verticality and environmental hazards.

## 4. The Core Game Loop

```mermaid
flowchart TD
    Hub["Sanctuary of the Anvil\n(Spend Generational Relics, Select Starting Blueprint)"] --> Ingress["Sector Ingress: Age 20\n(High Mobility, 2 Base Sockets)"]
    Ingress --> Chamber["Combat & Exploration Chamber\n(Real-Time Isometric Combat, Environmental Hazards)"]
    Chamber --> AIUpdate["Neural Blackboard Telemetry Update\n(Logs player weapon/movement habits)"]
    AIUpdate --> Reward{"Chamber Cleared?"}
    Reward -- Yes --> Loot["Draft Runic Glyphs & Currency"]
    Loot --> NextChamber["Next Modular Chamber (Enemies Counter-Evolve)"]
    NextChamber --> Chamber
    Reward -- No (Player Dies) --> AgeChoice{"Chrono-Revival Screen"}
    AgeChoice -- "Amnesia Purge" --> AgeUp["Age +10 Years\n(Reset Enemy AI Counters to Baseline)"]
    AgeChoice -- "Defiant Revival" --> AgeMinor["Age +2 Years (Relic Cost)\n(Enemies Retain Lethal Counters)"]
    AgeUp & AgeMinor --> CheckLifespan{"Current Age >= 80?"}
    CheckLifespan -- "No" --> Chamber
    CheckLifespan -- "Yes (Natural Death)" --> Legacy["Inherit Bloodline Legacy & Return to Sanctuary"]
    Legacy --> Hub
```
