package com.eternalcode.minions.minion;

import com.eternalcode.minions.behavior.MinionBehaviorDetails;
import com.eternalcode.minions.behavior.MinionBehaviorService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class MinionBehaviorServiceImpl implements MinionBehaviorService {

    private final MinionBehaviorRegistry behaviors;

    public MinionBehaviorServiceImpl(MinionBehaviorRegistry behaviors) {
        this.behaviors = behaviors;
    }

    @Override
    public MinionBehaviorDetails defaultBehavior() {
        return this.map(this.behaviors.defaultBehavior());
    }

    @Override
    public Optional<MinionBehaviorDetails> findById(String behaviorId) {
        return this.behaviors.find(behaviorId).map(this::map);
    }

    @Override
    public Collection<MinionBehaviorDetails> findAll() {
        List<MinionBehaviorDetails> details = new ArrayList<>(this.behaviors.behaviors().size());
        for (MinionBehavior behavior : this.behaviors.behaviors()) {
            details.add(this.map(behavior));
        }
        return List.copyOf(details);
    }

    private MinionBehaviorDetails map(MinionBehavior behavior) {
        return new MinionBehaviorDetails(behavior.id());
    }
}
