package com.eternalcode.minions.bridge.shop.impl.shopguiplus;

import net.brcdev.shopgui.event.ShopGUIPlusPostEnableEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ShopGuiPlusHookListener implements Listener {

    private final ShopGuiPlusShopIntegration integration;

    public ShopGuiPlusHookListener(ShopGuiPlusShopIntegration integration) {
        this.integration = integration;
    }

    @EventHandler
    public void onShopGuiPlusPostEnable(ShopGUIPlusPostEnableEvent event) {
        this.integration.markReady();
    }
}
