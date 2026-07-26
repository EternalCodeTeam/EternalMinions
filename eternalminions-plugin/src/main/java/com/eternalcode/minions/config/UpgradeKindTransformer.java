package com.eternalcode.minions.config;

import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;

public final class UpgradeKindTransformer extends BidirectionalTransformer<String, UpgradeKind> {

    @Override
    public GenericsPair<String, UpgradeKind> getPair() {
        return this.genericsPair(String.class, UpgradeKind.class);
    }

    @Override
    public UpgradeKind leftToRight(String data, SerdesContext context) {
        return new UpgradeKind(data);
    }

    @Override
    public String rightToLeft(UpgradeKind data, SerdesContext context) {
        return data.key();
    }
}
