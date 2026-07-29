package com.eternalcode.minions.bridge.vault;

import java.math.BigDecimal;
import java.util.UUID;

public interface EconomyService {

    boolean available();

    boolean withdraw(UUID playerId, BigDecimal amount);

    String format(BigDecimal amount);
}
