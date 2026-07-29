package com.eternalcode.minions.minion.upgrade;

import com.eternalcode.minions.bridge.vault.EconomyService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

final class UpgradePayment {

    private final Optional<? extends EconomyService> economy;

    UpgradePayment(Optional<? extends EconomyService> economy) {
        this.economy = economy;
    }

    boolean withdraw(UUID playerId, BigDecimal amount) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player id is required");
        }
        this.validateAmount(amount);

        EconomyService economyService = this.economy.orElse(null);
        if (economyService == null || !economyService.available()) {
            return false;
        }

        return economyService.withdraw(playerId, amount);
    }

    String format(BigDecimal amount) {
        this.validateAmount(amount);

        EconomyService economyService = this.economy.orElse(null);
        if (economyService == null || !economyService.available()) {
            return amount.toPlainString();
        }

        return economyService.format(amount);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Upgrade cost must be positive");
        }
    }
}
