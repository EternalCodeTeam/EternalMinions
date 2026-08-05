package com.eternalcode.minions.examples;

import com.eternalcode.minions.shop.MinionShopProvider;
import com.eternalcode.minions.shop.MinionShopRegistration;
import com.eternalcode.minions.shop.MinionShopService;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

final class MinionShopExamples implements AutoCloseable {

    private final Plugin plugin;
    private final MinionShopService shops;
    private final MinionShopProvider provider;
    private MinionShopRegistration registration;

    MinionShopExamples(Plugin plugin, MinionShopService shops) {
        this.plugin = plugin;
        this.shops = shops;
        this.provider = new LoggingShopProvider(plugin.getLogger());
    }

    boolean update(CommandSender sender, String[] arguments) {
        if (arguments.length < 2) {
            ExampleMessages.error(sender, "Usage: /minionapi shop <on|off|status>");
            return true;
        }

        return switch (arguments[1].toLowerCase(java.util.Locale.ROOT)) {
            case "on" -> this.enable(sender);
            case "off" -> this.disable(sender);
            case "status" -> this.status(sender);
            default -> {
                ExampleMessages.error(sender, "Use 'on', 'off' or 'status'.");
                yield true;
            }
        };
    }

    @Override
    public void close() {
        if (this.registration == null) {
            return;
        }
        this.registration.close();
        this.registration = null;
    }

    private boolean enable(CommandSender sender) {
        if (this.registration != null && this.registration.isRegistered()) {
            ExampleMessages.info(sender, "Demo shop provider is already active.");
            return true;
        }

        try {
            this.registration = this.shops.registerProvider(this.plugin, this.provider);
            ExampleMessages.success(sender, "Demo shop provider enabled. Payouts are logged only.");
        }
        catch (IllegalStateException exception) {
            ExampleMessages.error(sender, exception.getMessage());
        }
        return true;
    }

    private boolean disable(CommandSender sender) {
        if (this.registration == null) {
            ExampleMessages.info(sender, "Demo shop provider is not active.");
            return true;
        }
        this.close();
        ExampleMessages.success(sender, "Demo shop provider disabled.");
        return true;
    }

    private boolean status(CommandSender sender) {
        boolean registered = this.registration != null && this.registration.isRegistered();
        ExampleMessages.info(sender, "Demo shop provider active: " + registered);
        return true;
    }

    private static final class LoggingShopProvider implements MinionShopProvider {

        private final Logger logger;

        private LoggingShopProvider(Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public double priceOf(Material material) {
            return material.isAir() ? 0.0D : 1.0D;
        }

        @Override
        public void payout(UUID ownerId, double amount) {
            this.logger.info("Demo payout owner=" + ownerId + " amount=" + amount);
        }
    }
}
