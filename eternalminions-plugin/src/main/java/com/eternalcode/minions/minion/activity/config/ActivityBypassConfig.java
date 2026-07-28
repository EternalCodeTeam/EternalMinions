package com.eternalcode.minions.minion.activity.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import java.util.List;

public class ActivityBypassConfig extends OkaeriConfig {

    @Comment({
            "Permission that fully exempts a player's minions from every activity rule.",
            "Only takes effect while the owner is online (offline players cannot be permission-checked)."
    })
    public String permission = "eternalminions.activity.bypass";

    @Comment("Minion type ids that always ignore every activity rule, regardless of permission.")
    public List<String> exemptMinionTypes = List.of();
}
