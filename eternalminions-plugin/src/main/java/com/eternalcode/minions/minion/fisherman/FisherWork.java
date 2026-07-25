package com.eternalcode.minions.minion.fisherman;

// FISHERMAN-type tuning: how much contiguous water counts as a valid spot, how long a catch
// takes, and how much each level of Lure shortens that wait.
public record FisherWork(int minWaterBlocks, int baseWaitTicks, int lureTicksReductionPerLevel) {

    public FisherWork {
        if (minWaterBlocks < 1) {
            throw new IllegalArgumentException("Fisher work minimum water blocks must be positive");
        }
        if (baseWaitTicks < 1) {
            throw new IllegalArgumentException("Fisher work base wait ticks must be positive");
        }
        if (lureTicksReductionPerLevel < 0) {
            throw new IllegalArgumentException("Fisher work lure reduction must not be negative");
        }
    }
}
