package com.eternalcode.minions.integration;

import java.lang.reflect.Method;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

// Optional Vault economy bridge. Vault ships no artifact in this build, so the economy provider is
// resolved reflectively at runtime. Reflection here is deliberate and isolated: it runs at most once
// per seller action (every few seconds), never on a per-tick path, and degrades to a no-op when Vault
// or an economy plugin is absent.
public final class VaultEconomyHook {

    private final Server server;
    private Object economy;
    private Method depositPlayer;
    private boolean giveUp;

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
        try {
            this.depositPlayer.invoke(this.economy, player, amount);
            return true;
        }
        catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private void resolve() {
        if (this.economy != null || this.giveUp) {
            return;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = this.server.getServicesManager().getRegistration(economyClass);
            if (provider == null) {
                // Vault is present but no economy plugin has registered yet; retry on the next action.
                return;
            }
            this.economy = provider.getProvider();
            this.depositPlayer = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
        }
        catch (ClassNotFoundException missingVault) {
            this.giveUp = true;
        }
        catch (ReflectiveOperationException incompatibleVault) {
            this.giveUp = true;
        }
    }
}
