package com.eternalcode.minions.minion.behavior.impl.seller;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class SellerStatuses {

    public static final MinionStatus SELLING = MinionStatus.of("SELLING");
    public static final MinionStatus SHOP_NOT_LINKED = MinionStatus.of("SHOP_NOT_LINKED");
    public static final MinionStatus STORAGE_EMPTY = MinionStatus.of("STORAGE_EMPTY");
    public static final MinionStatus ITEM_HAS_NO_PRICE = MinionStatus.of("ITEM_HAS_NO_PRICE");
    public static final MinionStatus PAYOUT_FAILED = MinionStatus.of("PAYOUT_FAILED");

    private SellerStatuses() {
    }
}
