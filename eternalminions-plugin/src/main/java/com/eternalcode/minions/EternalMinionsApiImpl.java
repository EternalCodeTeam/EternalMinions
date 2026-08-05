package com.eternalcode.minions;

import com.eternalcode.minions.access.MinionAccessService;
import com.eternalcode.minions.behavior.MinionBehaviorService;
import com.eternalcode.minions.item.MinionItemService;
import com.eternalcode.minions.minion.MinionManagementService;
import com.eternalcode.minions.minion.MinionService;
import com.eternalcode.minions.shop.MinionShopService;
import com.eternalcode.minions.status.MinionStatusService;

record EternalMinionsApiImpl(
    MinionService minionService,
    MinionManagementService minionManagementService,
    MinionBehaviorService minionBehaviorService,
    MinionItemService minionItemService,
    MinionStatusService minionStatusService,
    MinionAccessService minionAccessService,
    MinionShopService minionShopService
) implements EternalMinionsApi {
}
