# Three-by-three Mining Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make each minion mine one block from a fair cyclic 3×3 layer below itself while armor stand and NPC renderers face and animate toward the selected block.

**Architecture:** Keep the nine-position cursor inside `ScheduledMinion`, with immutable static offset/yaw lookup data in a focused target selector. Pass the selected yaw through the existing renderer boundary. Use EntityLib packet entities only; armor-arm reset is coalesced in the renderer tick with a primitive map.

**Tech Stack:** Java 21, Paper 1.21.11, PacketEvents, EntityLib, FastUtil, JUnit 5, AssertJ

---

### Task 1: Cyclic 3×3 target selection

**Files:**
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/scheduler/MinionMiningTargets.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/scheduler/ScheduledMinion.java`
- Test: `eternalminions-plugin/src/test/java/com/eternalcode/minions/scheduler/MinionMiningTargetsTest.java`

- [ ] Write a failing test asserting all nine unique offsets are returned cyclically, at Y offset `-1`, with stable yaw values.
- [ ] Run `./gradlew :eternalminions-plugin:test --tests "*MinionMiningTargetsTest" --no-configuration-cache` and verify compilation/test failure because the selector does not exist.
- [ ] Implement static primitive offset/yaw arrays and cursor advancement without target collection allocation.
- [ ] Run the focused test and verify it passes.

### Task 2: Mining engine integration

**Files:**
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/scheduler/MinionActionEngine.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/render/MinionRenderer.java`
- Modify: renderer implementations under `eternalminions-plugin/src/main/java/com/eternalcode/minions/render/`

- [ ] Change a scheduled action to scan no more than nine cyclic targets and mine the first eligible loaded block.
- [ ] Advance the cursor after every inspected position so empty positions cannot starve later targets.
- [ ] Pass the precomputed target yaw to the renderer animation call.
- [ ] Compile the plugin and resolve all renderer implementations against the new animation contract.

### Task 3: Packet rotation and swing animation

**Files:**
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/render/AbstractEntityLibMinionRenderer.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/render/ArmorStandMinionRenderer.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/render/NpcMinionRenderer.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/render/RotatingHeadMinionRenderer.java`

- [ ] Rotate armor stand and NPC packet bodies by cloning their existing packet location, changing yaw, and teleporting once.
- [ ] Trigger main-hand swing for both living renderers.
- [ ] Track only active armor-arm animations in a `Long2LongOpenHashMap` and restore their pose from the shared renderer tick.
- [ ] Keep rotating-head behavior unchanged apart from accepting the yaw parameter.

### Task 4: Full verification

**Files:**
- Verify all project sources and generated shaded JAR.

- [ ] Run `./gradlew clean test build --no-configuration-cache` and require `BUILD SUCCESSFUL`.
- [ ] Start `:eternalminions-plugin:runServer`, verify successful Paper enable and clean database load, then stop the server cleanly.
- [ ] Scan sources for per-minion Bukkit tasks, streams in the mining path, and unintended renderer persistence.
- [ ] Do not commit or push any changes.
