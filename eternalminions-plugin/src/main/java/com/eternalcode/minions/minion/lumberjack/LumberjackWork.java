package com.eternalcode.minions.minion.lumberjack;

import org.bukkit.Material;

public record LumberjackWork(
        Material logMaterial,
        Material saplingMaterial,
        int maxLogsPerTree
) {

    private static final int MAX_ALLOWED_LOGS_PER_TREE = 4096;

    public LumberjackWork {
        if (logMaterial == null) {
            throw new IllegalArgumentException(
                    "Lumberjack work requires a log material"
            );
        }

        if (saplingMaterial == null) {
            throw new IllegalArgumentException(
                    "Lumberjack work requires a sapling material"
            );
        }

        if (maxLogsPerTree <= 0) {
            throw new IllegalArgumentException(
                    "Lumberjack work max logs per tree must be positive"
            );
        }

        if (maxLogsPerTree > MAX_ALLOWED_LOGS_PER_TREE) {
            throw new IllegalArgumentException(
                    "Lumberjack work max logs per tree cannot exceed "
                            + MAX_ALLOWED_LOGS_PER_TREE
            );
        }
    }
}