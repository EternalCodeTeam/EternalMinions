package com.eternalcode.minions.minion;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MinionService {

    Optional<MinionDetails> findById(MinionId minionId);

    Collection<MinionDetails> findByOwner(UUID ownerId);
}
