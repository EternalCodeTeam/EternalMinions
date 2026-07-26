package com.eternalcode.minions.config;

import com.eternalcode.minions.config.transformer.BukkitColorTransformer;
import com.eternalcode.minions.config.transformer.MinionStatusTransformer;
import com.eternalcode.minions.config.transformer.UpgradeKindTransformer;
import com.eternalcode.minions.config.transformer.XMaterialTransformer;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;

final class EternalMinionsSerdesPack implements OkaeriSerdesPack {

    @Override
    public void register(SerdesRegistry registry) {
        registry.register(new XMaterialTransformer());
        registry.register(new BukkitColorTransformer());
        registry.register(new MinionStatusTransformer());
        registry.register(new UpgradeKindTransformer());
    }
}
