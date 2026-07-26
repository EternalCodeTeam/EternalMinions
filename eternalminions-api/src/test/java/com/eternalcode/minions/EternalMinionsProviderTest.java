package com.eternalcode.minions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.eternalcode.minions.minion.MinionService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EternalMinionsProviderTest {

    @AfterEach
    void resetProvider() {
        EternalMinionsProvider.deinitialize();
    }

    @Test
    void rejectsAccessBeforeInitialization() {
        assertThatIllegalStateException()
            .isThrownBy(EternalMinionsProvider::provide)
            .withMessage("EternalMinionsApi has not been initialized yet!");
    }

    @Test
    void providesInitializedApi() {
        EternalMinionsApi api = new EmptyEternalMinionsApi();

        EternalMinionsProvider.initialize(api);

        assertThat(EternalMinionsProvider.provide()).isSameAs(api);
    }

    @Test
    void rejectsSecondInitialization() {
        EternalMinionsApi api = new EmptyEternalMinionsApi();
        EternalMinionsProvider.initialize(api);

        assertThatIllegalStateException()
            .isThrownBy(() -> EternalMinionsProvider.initialize(api))
            .withMessage("EternalMinionsApi has already been initialized!");
    }

    private static final class EmptyEternalMinionsApi implements EternalMinionsApi {

        @Override
        public MinionService minionService() {
            return new EmptyMinionService();
        }

        @Override
        public com.eternalcode.minions.access.MinionAccessService minionAccessService() {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.eternalcode.minions.shop.MinionShopService minionShopService() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class EmptyMinionService implements MinionService {

        @Override
        public Optional<com.eternalcode.minions.minion.MinionDetails> findById(
            com.eternalcode.minions.minion.MinionId minionId
        ) {
            return Optional.empty();
        }

        @Override
        public Collection<com.eternalcode.minions.minion.MinionDetails> findByOwner(UUID ownerId) {
            return List.of();
        }
    }
}
