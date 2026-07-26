package com.eternalcode.minions.bridge.vault;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultEconomyHook {

    private final Server server;
    private Economy economy;

    public VaultEconomyHook(Server server) {
        this.server = server;
    }

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
