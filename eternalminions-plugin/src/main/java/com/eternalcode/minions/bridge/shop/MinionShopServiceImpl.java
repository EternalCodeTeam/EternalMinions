package com.eternalcode.minions.bridge.shop;

import com.eternalcode.minions.shop.MinionShopProvider;
import com.eternalcode.minions.shop.MinionShopRegistration;
import com.eternalcode.minions.shop.MinionShopService;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

public final class MinionShopServiceImpl implements MinionShopService, MinionShopProvider, Listener {

    private final Logger logger;
    private final List<MinionShopProvider> builtInProviders;
    private final MinionShopProvider fallbackProvider = new NoopShopIntegration();

    private volatile Registration externalProvider;

    public MinionShopServiceImpl(Logger logger, List<MinionShopProvider> builtInProviders) {
        this.logger = logger;
        this.builtInProviders = List.copyOf(builtInProviders);
    }

    @Override
    public @NonNull MinionShopRegistration registerProvider(@NonNull Plugin plugin, @NonNull MinionShopProvider provider) {
        if (!plugin.isEnabled()) {
            throw new IllegalStateException(
                    "Cannot register a shop provider for disabled plugin: " + plugin.getName()
            );
        }

        Registration current = this.externalProvider;

        if (current != null && current.plugin != plugin) {
            throw new IllegalStateException(
                    "A shop provider is already registered by: " + current.plugin.getName()
            );
        }

        Registration registration = new Registration(plugin, provider);
        this.externalProvider = registration;

        return registration;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Registration registration = this.externalProvider;

        if (registration != null && registration.plugin == event.getPlugin()) {
            this.externalProvider = null;
        }
    }

    @Override
    public boolean available() {
        return this.resolveProvider() != this.fallbackProvider;
    }

    @Override
    public double priceOf(Material material) {
        try {
            return this.resolveProvider().priceOf(material);
        }
        catch (RuntimeException exception) {
            this.logger.log(
                    Level.WARNING,
                    "Shop provider failed to report the price of " + material,
                    exception
            );

            return 0.0D;
        }
    }

    @Override
    public void payout(UUID ownerId, double amount) {
        this.tryPayout(ownerId, amount);
    }

    @Override
    public boolean tryPayout(UUID ownerId, double amount) {
        try {
            return this.resolveProvider().tryPayout(ownerId, amount);
        }
        catch (RuntimeException exception) {
            this.logger.log(
                    Level.WARNING,
                    "Shop provider failed to pay out " + amount + " to " + ownerId,
                    exception
            );
            return false;
        }
    }

    private MinionShopProvider resolveProvider() {
        Registration registration = this.externalProvider;

        if (registration != null
                && registration.plugin.isEnabled()
                && this.isAvailable(registration.provider)) {
            return registration.provider;
        }

        for (MinionShopProvider provider : this.builtInProviders) {
            if (this.isAvailable(provider)) {
                return provider;
            }
        }

        return this.fallbackProvider;
    }

    private boolean isAvailable(MinionShopProvider provider) {
        try {
            return provider.available();
        }
        catch (RuntimeException exception) {
            this.logger.log(
                    Level.WARNING,
                    "Shop provider failed to report availability",
                    exception
            );

            return false;
        }
    }

    private final class Registration implements MinionShopRegistration {

        private final Plugin plugin;
        private final MinionShopProvider provider;

        private Registration(Plugin plugin, MinionShopProvider provider) {
            this.plugin = plugin;
            this.provider = provider;
        }

        @Override
        public void unregister() {
            if (this.isRegistered()) {
                MinionShopServiceImpl.this.externalProvider = null;
            }
        }

        @Override
        public boolean isRegistered() {
            return MinionShopServiceImpl.this.externalProvider == this;
        }
    }
}
