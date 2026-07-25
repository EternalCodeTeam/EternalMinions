package com.eternalcode.minions.config;

import com.eternalcode.minions.minion.status.MinionStatus;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;

public final class MinionStatusTransformer extends BidirectionalTransformer<String, MinionStatus> {

    @Override
    public GenericsPair<String, MinionStatus> getPair() {
        return this.genericsPair(String.class, MinionStatus.class);
    }

    @Override
    public MinionStatus leftToRight(String data, SerdesContext context) {
        return new MinionStatus(data);
    }

    @Override
    public String rightToLeft(MinionStatus data, SerdesContext context) {
        return data.key();
    }
}
