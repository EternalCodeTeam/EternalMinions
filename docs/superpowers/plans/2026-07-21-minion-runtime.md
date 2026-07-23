# EternalMinions Runtime Implementation Plan

**Goal:** Deliver a playable Paper-only minion runtime with persistent miner minions, one shared scheduler, an Inventory Framework panel, and ARMOR_STAND, ROTATING_HEAD, and packet-only NPC renderers.

**Architecture:** Keep the domain and lifecycle synchronous and simple. Database work runs on one injected IO executor and returns immutable data to the server thread. A single due-time scheduler processes only ready minions within a configurable action budget. Rendering is viewer-scoped: PacketEvents owns packets and interaction interception, EntityLib is isolated to hologram metadata/entities, Inventory Framework owns menus, and XSeries is used at configuration and item/material boundaries.

**Tech Stack:** Java 21, Paper 1.21.11, PacketEvents 2.13.0, EntityLib 3.3.6 snapshot, XSeries 13.7.1, Inventory Framework 0.12.0, HikariCP, ORMLite, Okaeri Configs, native Adventure, JUnit 6, AssertJ.

**Repository rule:** Do not create commits or push changes without the user's explicit permission.

---

## Task 1: Simplify the runtime domain

- Replace the lock-based snapshot registry with a server-thread-confined runtime registry.
- Store mutable internal `Minion` objects and expose immutable API snapshots.
- Keep one primary ID index; owner lookups scan the registry until profiling justifies a second index.
- Add explicit minion status, inventory, tool, action deadline, renderer type, and definition ID.
- Cover registration, removal, snapshot conversion, storage, and duplicate IDs with tests first.

## Task 2: Add scheduling

- Add a binary min-heap of due minions with stable tie-breaking.
- Run one Paper task and process only due entries under `physicalActionsPerTick` and `schedulerBudgetMicros`.
- Avoid streams, string creation, callbacks, and per-tick collection copies in the hot path.
- Reschedule a minion after its action; invalid/full minions use a slower retry deadline.
- Test ordering, rescheduling, cancellation, and work limits without a server.

## Task 3: Add persistence

- Adapt EternalCore's Hikari and ORMLite setup without copying its generic repository hierarchy.
- Support SQLite by default and MySQL, MariaDB, PostgreSQL, and H2 configuration.
- Store scalar minion state in `eternal_minions`; serialize tool/storage payloads to compact binary columns.
- Perform connect, schema work, load, save, delete, and batch flushes on the IO executor.
- Coalesce dirty minions and flush them periodically and during shutdown.
- Test table mapping and binary inventory codec independently.

## Task 4: Add PacketEvents and render lifecycle

- Treat PacketEvents as an external Paper dependency and initialize render services after it.
- Track viewers by join, quit, world change, chunk movement, minion add/remove, and renderer change.
- Do squared-distance checks only on movement/render reconciliation, never for every minion every tick.
- Maintain an entity-ID-to-minion-ID interaction index.
- Spawn and destroy client entities per viewer with no physical Bukkit entity.

## Task 5: Implement all three renderers

- `ARMOR_STAND`: small marker armor stand with configured head/tool and action arm animation.
- `ROTATING_HEAD`: small client-side display/head, deterministic rotation, and a short particle beam during work.
- `NPC`: fake player profile/entity packets, small scale, held pickaxe, and arm swing animation; no Bukkit/CraftBukkit entity.
- Cache immutable packet/item representations where PacketEvents permits it.
- Keep renderer-specific state behind three small renderer classes and one dispatcher.

## Task 6: Add EntityLib holograms

- Initialize EntityLib against the existing PacketEvents API.
- Implement holograms behind a `MinionHologramRenderer` adapter so EntityLib does not leak into domain code.
- Update hologram content only when minion state changes, not every tick.
- Dispose all wrapper entities and viewers during unload.

## Task 7: Add the minion item and placement lifecycle

- Create signed minion items using PersistentDataContainer.
- Use XSeries for configured material, head, sound, and particle resolution.
- Validate ownership, placement space, world, limits, and duplicate interaction before mutation.
- Persist creation/removal asynchronously and compensate in memory when persistence fails.
- Keep Bukkit listeners thin and delegate to placement/pickup services.

## Task 8: Add the Inventory Framework panel

- Build a native Adventure titled chest panel using Inventory Framework.
- Add tool, storage preview, collect-items, renderer selector, status, information, and pickup controls.
- Use descriptive Okaeri field names such as `minionPanelToolSlot`, `minionPanelCollectItemsButton`, and `minionStorageFull`.
- Make click handling transactional: validate first, mutate once, mark dirty once, redraw affected slots only.
- Add a config-driven GUI layout and XSeries material resolution.

## Task 9: Integrate the miner behavior

- Select a deterministic target block in front of the minion.
- Validate and break blocks only on the correct Paper thread.
- Apply tool semantics and place drops into the minion storage without spawning item entities.
- Trigger renderer animation separately from production logic.
- Pause cleanly when the chunk is unavailable, the target is invalid, or storage is full.

## Task 10: Verify the deliverable

- Run focused unit tests after every domain component.
- Run the complete Gradle test suite and shaded build.
- Inspect the shaded JAR for dependency relocation and Paper manifest correctness.
- Start a local Paper 1.21.11 server when dependencies permit and inspect startup/shutdown logs.
- Report remaining deferred work and do not commit.
