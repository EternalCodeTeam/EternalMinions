# EternalMinions: global renderer and normalized persistence

## Scope

The refactor removes renderer selection from individual minions and normalizes only data already used by the plugin. The plugin is not deployed, so the old development schema is discarded. There is no legacy migration, schema history, or speculative upgrade and economy schema.

## Domain model

`MinionRuntime` and `MinionSnapshot` are removed.

The internal `Minion` is an immutable aggregate composed of:

- immutable identity and placement fields: id, owner id, behavior id, position and active state;
- immutable `MinionProgress` containing the current level;
- immutable `MinionEquipment` containing the current tool;
- immutable `MinionStorage` containing the storage slots.

Changing active state, equipment, or storage creates a new `Minion` value. The registry atomically replaces the value stored under the same minion id. Bukkit `ItemStack` instances are cloned on input and output boundaries. Storage updates copy the small fixed slot array but reuse untouched private item values.

The public API exposes immutable `MinionDetails`, not a technical runtime object or snapshot. Persistence dirty tracking is kept outside domain objects. Mutable infrastructure is limited to the registry, scheduler, visibility index, and dirty-id tracker because these components intentionally manage changing runtime state.

## Global renderer

`MinionsConfig` contains one strongly typed `minionRenderer` value. Bootstrap resolves it once to the active `MinionRenderer`. Every render, animation, placement item, pickup item, and loaded minion uses that renderer.

`MinionRendererType` is an internal renderer/configuration type. It is removed from the minion aggregate, API, database, item PDC, placement logic, commands, GUI actions, and persisted state. Changing the config and restarting the server changes the renderer of every minion. Runtime renderer switching does not exist.

The viewer index stores only visible minion identifiers per player. The scheduler calls the selected renderer directly instead of resolving a type for every action.

## Database schema

The database is created from scratch with four tables:

- `eternal_minions`: id, owner id, behavior id, world key, block coordinates and active flag.
- `eternal_minion_progress`: minion id and current level.
- `eternal_minion_equipment`: generated row id, minion id, equipment slot and serialized item.
- `eternal_minion_storage`: generated row id, minion id, storage slot and serialized item.

Only occupied equipment and storage slots are stored. Item serialization remains binary because Bukkit item metadata is not relational data, but every item is an independent row. Updating one storage slot no longer depends on one aggregate storage BLOB.

There are no empty tables for future systems. Upgrades and economy receive separate schemas only after their real invariants are defined.

## Repository behavior

Initialization creates all four tables and DAOs on the database executor. Loading performs one query per table and assembles immutable minion aggregates in memory, avoiding N+1 queries.

A dirty-minion batch runs in one repository batch. For every minion it upserts the core and progress rows, replaces equipment rows, and replaces occupied storage rows. Deletion removes storage, equipment, progress, and finally the core row.

The main thread captures immutable persistence values before scheduling database work. Bukkit item serialization and deserialization remain on the main thread; the database executor receives only scalar values and defensively copied byte arrays.

## GUI and items

Renderer selection symbols, actions, labels, callbacks, and placeholders are removed from the minion panel. The default pattern contains only tool, storage, information, collect, pickup, and decoration elements.

The minion item has one boolean minion marker in PDC. `/minions give` gives one generic minion item. Placement no longer reads a renderer value, and pickup returns the same generic item.

## Error handling and tests

Repository failures use `MinionRepositoryException` through asynchronous futures. The repository does not become ready after partial initialization failure.

Tests cover immutable updates and defensive item boundaries, absence of renderer state, global renderer selection, normalized row assembly, independent storage rows, repository deletion, and configuration without renderer GUI actions. Full build and Paper startup verify the new H2 schema and clean lifecycle.
