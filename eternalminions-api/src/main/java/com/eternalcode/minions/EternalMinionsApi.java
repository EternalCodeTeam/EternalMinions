package com.eternalcode.minions;

import com.eternalcode.minions.access.MinionAccessService;
import com.eternalcode.minions.minion.MinionService;

public interface EternalMinionsApi {

    MinionService minionService();

    MinionAccessService minionAccessService();
}
