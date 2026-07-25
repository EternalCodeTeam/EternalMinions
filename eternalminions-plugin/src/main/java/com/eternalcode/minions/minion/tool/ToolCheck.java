package com.eternalcode.minions.minion.tool;

import com.eternalcode.minions.minion.status.MinionStatus;
import org.bukkit.inventory.ItemStack;

public sealed interface ToolCheck {

    record Ready(ItemStack tool) implements ToolCheck {

        public static final Ready EMPTY = new Ready(null);

        public Ready {
            tool = tool == null ? null : tool.clone();
        }

        @Override
        public ItemStack tool() {
            return this.tool == null ? null : this.tool.clone();
        }
    }

    record Stopped(MinionStatus reason) implements ToolCheck {

        public Stopped {
            if (reason == null) {
                throw new IllegalArgumentException("Stopped tool check requires a status reason");
            }
        }
    }
}
