package com.eternalcode.minions.bridge.vault;

import java.math.BigDecimal;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultEconomyHook implements EconomyService {

    private final Server server;
    private Economy economy;

    public VaultEconomyHook(Server server) {
        this.server = server;
    }

    @Override
    public boolean available() {
        this.resolve();
        return this.economy != null;
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!this.available()) {
            return false;
        }
        return this.economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean withdraw(UUID playerId, BigDecimal amount) {
        if (!this.available()) {
            return false;
        }

        double vaultAmount = amount.doubleValue();
        if (!Double.isFinite(vaultAmount) || vaultAmount <= 0.0D) {
            return false;
        }

        OfflinePlayer player = this.server.getOfflinePlayer(playerId);
        return this.economy.withdrawPlayer(player, vaultAmount).transactionSuccess();
    }

    @Override
    public String format(BigDecimal amount) {
        if (!this.available()) {
            return amount.toPlainString();
        }

        double vaultAmount = amount.doubleValue();
        if (!Double.isFinite(vaultAmount)) {
            return amount.toPlainString();
        }

        return this.economy.format(vaultAmount);
    }

    private void resolve() {
        if (this.economy != null) {
            return;
        }

        RegisteredServiceProvider<Economy> provider = this.server.getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            this.economy = provider.getProvider();
        }
    }
}
