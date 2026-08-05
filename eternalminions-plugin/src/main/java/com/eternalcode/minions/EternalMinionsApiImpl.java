package com.eternalcode.minions;

import com.eternalcode.minions.access.MinionAccessService;
import com.eternalcode.minions.behavior.MinionBehaviorService;
import com.eternalcode.minions.item.MinionItemService;
import com.eternalcode.minions.minion.MinionManagementService;
import com.eternalcode.minions.minion.MinionService;
import com.eternalcode.minions.shop.MinionShopService;
import com.eternalcode.minions.status.MinionStatusService;

final class EternalMinionsApiImpl implements EternalMinionsApi {

    private final MinionService minions;
    private final MinionManagementService management;
    private final MinionBehaviorService behaviors;
    private final MinionItemService items;
    private final MinionStatusService statuses;
    private final MinionAccessService access;
    private final MinionShopService shop;

    EternalMinionsApiImpl(
        MinionService minions,
        MinionManagementService management,
        MinionBehaviorService behaviors,
        MinionItemService items,
        MinionStatusService statuses,
        MinionAccessService access,
        MinionShopService shop
    ) {
        this.minions = minions;
        this.management = management;
        this.behaviors = behaviors;
        this.items = items;
        this.statuses = statuses;
        this.access = access;
        this.shop = shop;
    }

    @Override
    public MinionService minionService() {
        return this.minions;
    }

    @Override
    public MinionManagementService minionManagementService() {
        return this.management;
    }

    @Override
    public MinionBehaviorService minionBehaviorService() {
        return this.behaviors;
    }

    @Override
    public MinionItemService minionItemService() {
        return this.items;
    }

    @Override
    public MinionStatusService minionStatusService() {
        return this.statuses;
    }

    @Override
    public MinionAccessService minionAccessService() {
        return this.access;
    }

    @Override
    public MinionShopService minionShopService() {
        return this.shop;
    }
}
