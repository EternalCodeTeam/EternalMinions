package com.eternalcode.minions;

import com.eternalcode.minions.access.MinionAccessService;
import com.eternalcode.minions.minion.MinionService;
import com.eternalcode.minions.shop.MinionShopService;

public interface EternalMinionsApi {

    MinionService minionService();

    MinionAccessService minionAccessService();

    MinionShopService minionShopService();
}
