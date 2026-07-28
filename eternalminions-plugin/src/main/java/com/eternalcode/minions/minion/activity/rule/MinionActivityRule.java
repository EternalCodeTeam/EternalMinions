package com.eternalcode.minions.minion.activity.rule;

import com.eternalcode.minions.minion.activity.ActivityDecision;
import com.eternalcode.minions.minion.activity.MinionActivityContext;

public interface MinionActivityRule {

    ActivityDecision evaluate(MinionActivityContext context);
}
