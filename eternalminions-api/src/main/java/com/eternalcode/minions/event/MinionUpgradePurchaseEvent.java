package com.eternalcode.minions.event;

import com.eternalcode.minions.minion.MinionSnapshot;
import java.math.BigDecimal;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired before payment for a player upgrade. Cancelling prevents payment and the upgrade. */
public final class MinionUpgradePurchaseEvent extends MinionEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final MinionSnapshot minion;
    private final String upgradeId;
    private final int currentTier;
    private final int targetTier;
    private final BigDecimal price;
    private boolean cancelled;

    public MinionUpgradePurchaseEvent(
        @NotNull MinionSnapshot minion,
        @NotNull UUID playerId,
        @NotNull String upgradeId,
        int currentTier,
        int targetTier,
        @NotNull BigDecimal price
    ) {
        super(MinionEventCause.UPGRADE_PURCHASE, playerId);
        if (minion == null || upgradeId == null || upgradeId.isBlank() || price == null) {
            throw new IllegalArgumentException("Minion, upgrade id and price are required");
        }
        this.minion = minion;
        this.upgradeId = upgradeId;
        this.currentTier = currentTier;
        this.targetTier = targetTier;
        this.price = price;
    }

    public @NotNull MinionSnapshot minion() { return this.minion; }
    public @NotNull String upgradeId() { return this.upgradeId; }
    public int currentTier() { return this.currentTier; }
    public int targetTier() { return this.targetTier; }
    public @NotNull BigDecimal price() { return this.price; }

    @Override
    public boolean isCancelled() { return this.cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
