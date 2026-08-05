package com.eternalcode.minions.examples;

import com.eternalcode.minions.EternalMinionsApi;
import com.eternalcode.minions.EternalMinionsProvider;
import com.eternalcode.minions.access.MinionAccessRegistration;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EternalMinionsApiExamplesPlugin extends JavaPlugin {

    private MinionAccessRegistration accessRegistration;
    private MinionShopExamples shopExamples;

    @Override
    public void onEnable() {
        EternalMinionsApi api = EternalMinionsProvider.provide();
        this.getServer().getPluginManager().registerEvents(
            new MinionEventExamples(this.getServer(), this.getLogger()),
            this
        );
        this.shopExamples = new MinionShopExamples(this, api.minionShopService());
        this.accessRegistration = api.minionAccessService().registerPolicy(
            this,
            new PermissionAccessPolicy()
        );

        PluginCommand command = Objects.requireNonNull(
            this.getCommand("minionapi"),
            "The minionapi command is missing from plugin metadata"
        );
        MinionApiCommand apiCommand = new MinionApiCommand(api, this.shopExamples);
        command.setExecutor(apiCommand);
        command.setTabCompleter(apiCommand);
    }

    @Override
    public void onDisable() {
        if (this.shopExamples != null) {
            this.shopExamples.close();
            this.shopExamples = null;
        }
        if (this.accessRegistration == null) {
            return;
        }
        this.accessRegistration.close();
        this.accessRegistration = null;
    }
}
