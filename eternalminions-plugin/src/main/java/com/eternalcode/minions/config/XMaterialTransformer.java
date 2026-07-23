package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;

public final class XMaterialTransformer extends BidirectionalTransformer<String, XMaterial> {

    @Override
    public GenericsPair<String, XMaterial> getPair() {
        return this.genericsPair(String.class, XMaterial.class);
    }

    @Override
    public XMaterial leftToRight(String data, SerdesContext context) {
        return XMaterial.matchXMaterial(data)
            .orElseThrow(() -> new IllegalArgumentException("Unknown XMaterial: " + data));
    }

    @Override
    public String rightToLeft(XMaterial data, SerdesContext context) {
        return data.name();
    }
}
