package com.eternalcode.minions.minion.seller;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class SellerStatuses {

    public static final MinionStatus SELLING = new MinionStatus("SELLING");
    public static final MinionStatus SHOP_NOT_LINKED = new MinionStatus("SHOP_NOT_LINKED");
    public static final MinionStatus STORAGE_EMPTY = new MinionStatus("STORAGE_EMPTY");
    public static final MinionStatus ITEM_HAS_NO_PRICE = new MinionStatus("ITEM_HAS_NO_PRICE");

    private SellerStatuses() {
    }
}
