# Three-by-three mining animation design

## Behavior

Each active minion mines one eligible block per action from the 3×3 layer directly below its placement position. The target Y coordinate is `minionY - 1`; X and Z range from `-1` to `1` around the minion. Air, liquid, and bedrock are skipped. Selection advances cyclically so every position gets a fair turn and regrown blocks are revisited.

## Runtime state

`ScheduledMinion` owns a primitive cursor from `0` to `8`. Target offsets and their yaw values are static precomputed arrays. A due action checks at most nine blocks and creates no target collection. The cursor is runtime scheduling state and is not persisted.

## Rendering

The renderer receives the selected target yaw with the animation request. Armor stand and NPC bodies rotate through EntityLib packet teleportation and perform a main-hand swing. Armor stand uses an arm pose with a shared renderer tick restoring the resting pose; it does not create a Bukkit task per minion. Rotating head retains its current animation.

## Performance

Only due minions inspect blocks. A single action performs at most nine direct block lookups. Static primitive arrays avoid target objects, streams, iterators, and trigonometry in the hot path. Animation expiry uses a primitive FastUtil map and is processed only for currently animating armor stands.

## Verification

Unit tests cover cyclic 3×3 target order and yaw mapping. Existing scheduler, persistence, registry, config, and API tests must remain green. The shaded plugin must build and start on Paper 1.21.11.
