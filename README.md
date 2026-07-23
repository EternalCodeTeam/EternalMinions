# EternalMinions

Paper-only minion automation for Java 21 and Minecraft 1.21.11.

## Current implementation

- `ARMOR_STAND`: small packet armor stand with a head, pickaxe, and arm animation.
- `ROTATING_HEAD`: scaled item display with cached rotations and an interaction hitbox.
- `NPC`: packet-only 1.21.11 mannequin with a pickaxe, scale attribute, and hand swing.
- EntityLib text-display holograms shared between viewers.
- PacketEvents viewer lifecycle and packet interaction interception.
- One allocation-light binary heap scheduler with action and time budgets.
- Miner behavior that collects drops directly into nine virtual storage slots.
- Inventory Framework panel for tool insertion, storage, collection, renderer selection, and pickup.
- XSeries material handling for configured and generated items.
- HikariCP and ORMLite persistence with H2 by default and MySQL-compatible MariaDB/PostgreSQL options.
- Binary `ItemStack` payloads for the tool and storage.
- Batched asynchronous dirty writes and a bounded shutdown flush.
- Native Paper Adventure messages through Multification.

The runtime registry is deliberately server-thread confined. It keeps one primitive ID index and scans for cold owner queries instead of maintaining locks and duplicate indexes.

## Modules

- `eternalminions-api`: immutable public snapshots and service provider.
- `eternalminions-plugin`: runtime, persistence, renderers, GUI, commands, and listeners.
- `buildSrc`: Java 21 conventions and centralized dependency versions.

## Requirements

- Java 21
- Paper 1.21.11
- PacketEvents 2.13.0 installed as a server plugin

## Quick test

1. Run `/minions give` with `eternalminions.command.give`.
2. Place one of the three generated minion items against a block.
3. Click the packet entity to open its panel.
4. Put a tool in the tool slot and place a breakable block one block south of the minion.

## Building

```shell
./gradlew clean test build
```

The shaded artifact is generated under `eternalminions-plugin/build/libs/`.

## Deferred work

- configurable behavior definitions beyond the first miner;
- fuel and upgrade pipelines;
- protection-plugin adapters and block-regeneration strategies;
- configurable skins and heads;
- offline production mathematics;
- load benchmarks used to decide whether the binary heap should be replaced by a timing wheel.
