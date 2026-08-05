# EternalMinions API examples

This module is a standalone Paper plugin that demonstrates every currently implemented API
service. It depends only on `eternalminions-api` at compile time.

## Run locally

From the repository root:

```powershell
.\gradlew.bat :eternalminions-api-examples:runServer
```

## Commands

- `/minionapi behaviors` - behavior catalog and default behavior.
- `/minionapi owned` - query minions owned by the executing player.
- `/minionapi snapshot <id>` - complete immutable state snapshot.
- `/minionapi at` - exact block-position lookup.
- `/minionapi give <behavior>` - create a portable minion item.
- `/minionapi inspect` - inspect the minion item in the main hand.
- `/minionapi create <behavior>` - create a minion above the targeted block.
- `/minionapi remove <id>` - remove a minion from runtime and persistence.
- `/minionapi rotate <id>` - rotate a minion through the management API.
- `/minionapi chest <id> <here|clear>` - set or clear a chest position.
- `/minionapi tool <id>` - copy the main-hand item into minion equipment, or clear it with air.
- `/minionapi storage <id> <slot>` - copy the main-hand item into a storage slot.
- `/minionapi upgrade <id> <kind> <tier>` - set an upgrade tier.
- `/minionapi progress <id> <level> <value>` - set level and progress.
- `/minionapi status <id> [KEY|clear]` - read, override or clear runtime status.
- `/minionapi access <id> <action>` - evaluate owner plus registered access policies.
- `/minionapi shop <on|off|status>` - control a demo shop provider whose payouts are logged only.

The plugin also registers `PermissionAccessPolicy`: players with
`eternalminions.examples.manage-any` gain access to every minion. All example commands require the
operator-only `eternalminions.examples.command` permission by default.

`MinionEventExamples` is registered automatically and demonstrates listening to create, remove and
typed before/after update events. Giving a player
`eternalminions.examples.block-upgrades` demonstrates cancelling `MinionUpgradePurchaseEvent`
before money is withdrawn.

The demo shop provider is never enabled automatically. When manually enabled, it takes precedence
over built-in shop bridges, assigns a price of `1.0` to every non-air material and only logs payout
requests. It is intended for API verification, not production economy use.

## Event listeners

Events are regular Bukkit events and are registered with the consuming plugin:

```java
plugin.getServer().getPluginManager().registerEvents(listener, plugin);
```

Available event examples cover:

- `MinionPreCreateEvent` and `MinionCreatedEvent`
- `MinionPreRemoveEvent` and `MinionRemovedEvent`
- `MinionUpdatedEvent` with `previous()`, `current()` and `updateType()`
- cancellable `MinionUpgradePurchaseEvent`, fired before payment

Every event exposes `cause()` and nullable `actorId()`. All event snapshots are immutable API
models and all events run synchronously on the server thread.
