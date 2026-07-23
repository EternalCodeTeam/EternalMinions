# Global Renderer and Normalized Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Do not commit or push without the user's explicit permission.

**Goal:** Replace per-minion renderer state and the wide persistence table with one global renderer, immutable minion values, and four normalized tables.

**Architecture:** Immutable domain values are replaced atomically in `MinionRegistry`; mutable scheduling, visibility, and dirty-version tracking remain isolated infrastructure. Bootstrap selects one renderer from typed configuration. Persistence captures immutable scalar and binary values on the main thread and writes four tables asynchronously in batches.

**Tech Stack:** Java 21, Paper 1.21.11, Adventure, PacketEvents, EntityLib, XSeries, Okaeri Configs, ORMLite, HikariCP, fastutil, JUnit 6, AssertJ.

---

## File structure

- Replace API `MinionSnapshot` with immutable `MinionDetails`.
- Replace plugin `MinionRuntime` with immutable `Minion` composed from `MinionProgress`, `MinionEquipment`, and `MinionStorage`.
- Add `MinionStorageUpdate` for allocation-explicit immutable storage writes.
- Move `MinionRendererType` into the plugin render package and select one renderer during bootstrap.
- Remove `MinionRendererRegistry`; inject the selected `MinionRenderer` directly.
- Keep `MinionRegistry` responsible only for identifier-to-minion lookup and atomic replacement.
- Add `DirtyMinionTracker` for persistence versions.
- Split `MinionTable` into core, progress, equipment, and storage ORMLite rows.

### Task 1: Immutable domain and API

**Files:**
- Delete: `eternalminions-api/src/main/java/com/eternalcode/minions/minion/MinionSnapshot.java`
- Create: `eternalminions-api/src/main/java/com/eternalcode/minions/minion/MinionDetails.java`
- Delete: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionRuntime.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/Minion.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionProgress.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionEquipment.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionStorage.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionStorageUpdate.java`
- Modify: `eternalminions-api/src/main/java/com/eternalcode/minions/minion/MinionService.java`
- Replace test: `eternalminions-plugin/src/test/java/com/eternalcode/minions/minion/MinionRuntimeTest.java`
- Modify test: `eternalminions-plugin/src/test/java/com/eternalcode/minions/minion/MinionRegistryTest.java`

- [ ] **Step 1: Write failing immutable model tests**

Test that `withEquipment`, `withStorage`, and `withActive` return new values without changing the original, and that returned `ItemStack` values are defensive clones:

```java
@Test
void replacesEquipmentWithoutMutatingOriginalMinion() {
    Minion original = minion();
    ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);

    Minion updated = original.withEquipment(new MinionEquipment(pickaxe));
    pickaxe.setAmount(2);

    assertThat(original.equipment().tool()).isNull();
    assertThat(updated.equipment().tool()).hasSameTypeAs(new ItemStack(Material.DIAMOND_PICKAXE));
    assertThat(updated).isNotSameAs(original);
}
```

- [ ] **Step 2: Run the domain tests and verify RED**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test --tests "com.eternalcode.minions.minion.*"
```

Expected: compilation fails because `Minion`, `MinionProgress`, `MinionEquipment`, and immutable storage APIs do not exist.

- [ ] **Step 3: Implement immutable values**

Use final fields and replacement methods. Keep `ItemStack` private through cloning:

```java
public final class Minion {
    private final MinionId id;
    private final UUID ownerId;
    private final String behaviorId;
    private final MinionPosition position;
    private final boolean active;
    private final MinionProgress progress;
    private final MinionEquipment equipment;
    private final MinionStorage storage;

    public Minion withStorage(MinionStorage storage) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position,
            this.active, this.progress, this.equipment, storage);
    }
}
```

`MinionStorage.add` returns `MinionStorageUpdate(updatedStorage, remainingItem)` and never mutates the receiver.

- [ ] **Step 4: Replace API snapshot terminology**

`MinionDetails` is an immutable API record containing id, owner, behavior, position, level, and active state. `MinionService` returns `Optional<MinionDetails>` and collections of `MinionDetails`.

- [ ] **Step 5: Update `MinionRegistry` to replace immutable values**

Keep one primitive map and direct operations:

```java
public void replace(Minion minion) {
    if (!this.minions.containsKey(minion.id().value())) {
        throw new IllegalArgumentException("Minion " + minion.id().value() + " is not registered");
    }
    this.minions.put(minion.id().value(), minion);
}
```

- [ ] **Step 6: Run domain and registry tests and verify GREEN**

Run the command from Step 2. Expected: all minion model and registry tests pass.

### Task 2: One global renderer

**Files:**
- Delete: `eternalminions-api/src/main/java/com/eternalcode/minions/minion/MinionRendererType.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/render/MinionRendererType.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/config/MinionsConfig.java`
- Delete: `eternalminions-plugin/src/main/java/com/eternalcode/minions/render/MinionRendererRegistry.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/render/MinionRenderService.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/scheduler/MinionActionEngine.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/EternalMinionsPlugin.java`
- Create test: `eternalminions-plugin/src/test/java/com/eternalcode/minions/render/MinionRendererTypeTest.java`

- [ ] **Step 1: Write a failing renderer selection test**

```java
@Test
void resolvesConfiguredRendererOnce() {
    MinionsConfig config = new MinionsConfig();
    config.minionRenderer = MinionRendererType.NPC;

    assertThat(config.minionRenderer).isEqualTo(MinionRendererType.NPC);
}
```

Also add compilation assertions indirectly by removing all `rendererType()` expectations from domain tests.

- [ ] **Step 2: Run the renderer test and verify RED**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test --tests "com.eternalcode.minions.render.*"
```

Expected: failure because the internal enum and global config field do not exist.

- [ ] **Step 3: Add the typed global configuration**

```java
@Comment("Renderer used by every minion. Changing it requires a server restart.")
public MinionRendererType minionRenderer = MinionRendererType.ARMOR_STAND;
```

- [ ] **Step 4: Select the renderer in bootstrap**

Construct all required dependencies only for the selected branch and keep one `MinionRenderer` reference:

```java
MinionRenderer renderer = switch (minionsConfig.minionRenderer) {
    case ARMOR_STAND -> new ArmorStandMinionRenderer(holograms, entityIndex);
    case ROTATING_HEAD -> new RotatingHeadMinionRenderer(
        holograms, entityIndex, this.getServer(), minionsConfig.animationDistanceBlocks);
    case NPC -> new NpcMinionRenderer(holograms, entityIndex);
};
```

- [ ] **Step 5: Simplify rendering and scheduling**

`MinionRenderService` stores `LongOpenHashSet` per player and calls the injected renderer directly. `MinionActionEngine` calls `renderer.animate(minion.id())`. Remove renderer switching, previous-renderer tracking, registry lookups, and ticking of inactive renderer implementations.

- [ ] **Step 6: Run renderer tests and compile**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test --tests "com.eternalcode.minions.render.*" :eternalminions-plugin:compileJava
```

Expected: renderer tests pass and no production reference to per-minion renderer state remains.

### Task 3: Remove renderer from GUI, items, and placement

**Files:**
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/config/MinionPanelAction.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/config/MinionPanelConfig.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/config/MinionPanelElementConfig.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/config/MinionPanelLayout.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/gui/MinionPanel.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/item/MinionItemFactory.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionPlacementListener.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/command/MinionGiveCommand.java`
- Modify tests: `eternalminions-plugin/src/test/java/com/eternalcode/minions/config/MinionPanelLayoutTest.java`
- Create test: `eternalminions-plugin/src/test/java/com/eternalcode/minions/item/MinionItemFactoryTest.java`

- [ ] **Step 1: Write failing config contract tests**

Assert that the default pattern contains no renderer action and that panel elements cannot carry `rendererType`:

```java
@Test
void defaultPanelContainsNoRendererSelection() {
    MinionPanelConfig config = new MinionPanelConfig();
    assertThat(config.elements.values())
        .extracting(element -> element.action)
        .doesNotContain(MinionPanelAction.SELECT_RENDERER);
}
```

- [ ] **Step 2: Run the panel tests and verify RED**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test --tests "com.eternalcode.minions.config.MinionPanelLayoutTest"
```

Expected: the default config still exposes renderer selectors.

- [ ] **Step 3: Remove renderer GUI configuration and callbacks**

Delete `SELECT_RENDERER`, `rendererType`, renderer state strings, renderer buttons, `{MINION_RENDERER}`, `BiConsumer<Minion, MinionRendererType>`, and renderer click handling. Use a compact bottom row containing collect and pickup actions.

- [ ] **Step 4: Make the minion item generic**

Use one byte marker:

```java
public boolean isMinion(ItemStack item) {
    if (item == null || item.getType().isAir()) {
        return false;
    }
    Byte marker = item.getPersistentDataContainer().get(this.minionKey, PersistentDataType.BYTE);
    return marker != null && marker == (byte) 1;
}
```

`create()` has no renderer argument, `/minions give` gives one item, placement checks `isMinion`, and pickup returns `create()`.

- [ ] **Step 5: Run panel and item tests and verify GREEN**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test --tests "com.eternalcode.minions.config.*" --tests "com.eternalcode.minions.item.*"
```

Expected: all selected tests pass.

### Task 4: Normalized persistence values and rows

**Files:**
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/MinionData.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/MinionTable.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/MinionProgressTable.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/MinionEquipmentTable.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/MinionStorageTable.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/StoredItemData.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/ItemDataCodec.java`
- Create test: `eternalminions-plugin/src/test/java/com/eternalcode/minions/database/MinionDataTest.java`

- [ ] **Step 1: Write failing normalized capture/restore tests**

Create a minion with a tool and items in non-contiguous storage slots. Assert `MinionData.capture` produces one equipment value and independent `(slot, bytes)` storage entries, and `restore` recreates an equal immutable aggregate.

```java
assertThat(data.storageItems())
    .extracting(StoredItemData::slot)
    .containsExactly(0, 4, 8);
assertThat(data.rendererType()).isNotPresent();
```

The renderer assertion should be expressed by compilation/API shape rather than adding an optional renderer field.

- [ ] **Step 2: Run database model tests and verify RED**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test --tests "com.eternalcode.minions.database.MinionDataTest"
```

Expected: failure because normalized storage data and row types do not exist.

- [ ] **Step 3: Define four ORMLite rows**

Use table names exactly:

```java
@DatabaseTable(tableName = "eternal_minions")
final class MinionTable { }

@DatabaseTable(tableName = "eternal_minion_progress")
final class MinionProgressTable { }

@DatabaseTable(tableName = "eternal_minion_equipment")
final class MinionEquipmentTable { }

@DatabaseTable(tableName = "eternal_minion_storage")
final class MinionStorageTable { }
```

Core contains no renderer, level, tool, or storage BLOB. Progress uses `minionId` as its id. Equipment and storage use generated row ids plus `uniqueCombo` on `(minionId, slot)`.

- [ ] **Step 4: Capture and restore normalized immutable values**

`MinionData` contains core scalars, level, optional tool bytes, and immutable `List<StoredItemData>`. Clone all byte arrays at record boundaries. `ItemDataCodec` serializes one item only; remove aggregate array encoding.

- [ ] **Step 5: Run normalized model tests and verify GREEN**

Run the command from Step 2. Expected: all database model tests pass.

### Task 5: Four-table repository and external dirty tracking

**Files:**
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/MinionRepository.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/MinionPersistenceService.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/database/DirtyMinionTracker.java`
- Create test: `eternalminions-plugin/src/test/java/com/eternalcode/minions/database/DirtyMinionTrackerTest.java`
- Create integration test: `eternalminions-plugin/src/test/java/com/eternalcode/minions/database/MinionRepositoryTest.java`

- [ ] **Step 1: Write failing dirty-version tests**

```java
@Test
void keepsMinionDirtyWhenItChangesDuringSave() {
    DirtyMinionTracker tracker = new DirtyMinionTracker();
    long savedVersion = tracker.changed(new MinionId(7));
    tracker.changed(new MinionId(7));

    tracker.markSaved(new MinionId(7), savedVersion);

    assertThat(tracker.isDirty(new MinionId(7))).isTrue();
}
```

- [ ] **Step 2: Run tracker tests and verify RED**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test --tests "com.eternalcode.minions.database.DirtyMinionTrackerTest"
```

Expected: compilation fails because the tracker does not exist.

- [ ] **Step 3: Implement the tracker with primitive maps**

Use `Long2LongOpenHashMap` for current and persisted versions. Keep it main-thread-owned; async callbacks schedule the final `markSaved` call back onto Paper's main thread.

- [ ] **Step 4: Write failing real H2 repository tests**

Use a temporary in-memory H2 configuration. Initialize the repository, save one minion, query the four tables through the repository, reload it, then delete it. Assert one core row, one progress row, one equipment row, three storage rows, and zero rows after deletion.

- [ ] **Step 5: Run repository tests and verify RED**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test --tests "com.eternalcode.minions.database.MinionRepositoryTest"
```

Expected: failure because the repository still creates and writes one table.

- [ ] **Step 6: Initialize and query four DAOs**

Create all tables before setting `ready = true`. `loadAll` performs exactly four `queryForAll` calls and assembles values by minion id. Missing progress is a repository consistency error; absent equipment/storage means empty values.

- [ ] **Step 7: Batch save and ordered delete**

For every minion in one `callBatchTasks` block:

1. `createOrUpdate` core row.
2. `createOrUpdate` progress row.
3. delete equipment rows by minion id, then insert occupied equipment.
4. delete storage rows by minion id, then insert occupied slots.

Delete storage, equipment, progress, and core rows in that order.

- [ ] **Step 8: Connect persistence service to immutable replacements**

Capture `MinionData` on the main thread. The async repository receives immutable data. On successful completion, schedule `tracker.markSaved(id, capturedVersion)` on the main thread.

- [ ] **Step 9: Run tracker and repository tests and verify GREEN**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test --tests "com.eternalcode.minions.database.*"
```

Expected: all database tests pass without thread or resource warnings.

### Task 6: Wire immutable updates through gameplay

**Files:**
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/scheduler/MinionActionEngine.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/gui/MinionPanel.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionLifecycleService.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionPlacementListener.java`
- Modify: `eternalminions-plugin/src/main/java/com/eternalcode/minions/EternalMinionsPlugin.java`
- Modify tests: all existing minion, scheduler, command, and GUI tests affected by type changes.

- [ ] **Step 1: Write failing immutable gameplay update tests**

Test a small pure update service or extracted operation that replaces storage/tool state in the registry and marks the id dirty. Assert the previous `Minion` remains unchanged and registry returns the replacement.

- [ ] **Step 2: Run affected tests and verify RED**

Run:

```powershell
.\gradlew.bat :eternalminions-plugin:test
```

Expected: compilation failures at remaining `MinionRuntime`, `MinionSnapshot`, and renderer-type call sites.

- [ ] **Step 3: Replace gameplay mutation paths**

The action engine calculates `MinionStorageUpdate`, creates `updatedMinion`, replaces it in the registry, marks it dirty, and animates through the global renderer. The panel performs the same replacement pattern for tool changes and collection. Lifecycle placement creates one immutable aggregate; pickup deletes it and returns a generic item.

- [ ] **Step 4: Remove obsolete code completely**

Run:

```powershell
rg -n "MinionRuntime|MinionSnapshot|changeRenderer|rendererType\(\)|SELECT_RENDERER|minion_renderer|storageData|toolData|MinionRendererRegistry|schema_migration" eternalminions-api/src eternalminions-plugin/src -g "*.java"
```

Expected: no matches except intentional `toolData` names inside normalized persistence values if retained. Rename those to `serializedTool` if necessary so the result is unambiguous.

- [ ] **Step 5: Run all tests without cache**

Run:

```powershell
.\gradlew.bat clean test build --rerun-tasks
```

Expected: all tests pass and shadow JAR is created.

- [ ] **Step 6: Reset only the local non-production H2 test database**

Stop the test server, resolve the exact files under `eternalminions-plugin/run/plugins/EternalMinions/` belonging to the H2 `minions` database, verify every resolved path stays under that directory, and remove only those local development database files. Do not touch external database configurations.

- [ ] **Step 7: Boot Paper and inspect schema**

Run Paper 1.21.11, wait for repository readiness, execute `/minions reload`, and stop cleanly. Query the local H2 metadata and verify exactly the four EternalMinions domain tables exist with no renderer column and no aggregate storage/tool columns.

- [ ] **Step 8: Final review**

Verify no commits were created, no unrelated files changed, no renderer selector remains in generated `panel.yml`, and the final JAR path is `eternalminions-plugin/build/libs/EternalMinions-v0.1.0-SNAPSHOT.jar`.
