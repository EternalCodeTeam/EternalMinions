package com.eternalcode.minions.minion.upgrade;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_upgrades")
public final class MinionUpgradeTable {

    public static final String MINION_ID_COLUMN = "minion_id";
    public static final String TYPE_COLUMN = "upgrade_type";
    public static final String TIER_COLUMN = "tier";

    @DatabaseField(columnName = MINION_ID_COLUMN, uniqueCombo = true)
    private long minionId;

    @DatabaseField(columnName = TYPE_COLUMN, canBeNull = false, uniqueCombo = true)
    private String upgradeType;

    @DatabaseField(columnName = TIER_COLUMN)
    private int tier;

    public MinionUpgradeTable() {
    }

    public MinionUpgradeTable(long minionId, String upgradeType, int tier) {
        this.minionId = minionId;
        this.upgradeType = upgradeType;
        this.tier = tier;
    }

    public long minionId() {
        return this.minionId;
    }

    public String upgradeType() {
        return this.upgradeType;
    }

    public int tier() {
        return this.tier;
    }
}
