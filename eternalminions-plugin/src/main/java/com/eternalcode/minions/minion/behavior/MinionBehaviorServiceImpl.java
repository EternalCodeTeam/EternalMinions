package com.eternalcode.minions.minion.behavior;

import com.eternalcode.minions.behavior.MinionBehaviorDetails;
import com.eternalcode.minions.behavior.MinionBehaviorService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public final class MinionBehaviorServiceImpl implements MinionBehaviorService {

    private final MinionBehaviorRegistry behaviors;

    public MinionBehaviorServiceImpl(MinionBehaviorRegistry behaviors) {
        this.behaviors = behaviors;
    }

    @Override
    public @NonNull MinionBehaviorDetails defaultBehavior() {
        return this.map(this.behaviors.defaultBehavior());
    }

    @Override
    public @NonNull Optional<MinionBehaviorDetails> findById(@NonNull String behaviorId) {
        return this.behaviors.find(behaviorId).map(this::map);
    }

    @Override
    public @NonNull Collection<MinionBehaviorDetails> findAll() {
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
