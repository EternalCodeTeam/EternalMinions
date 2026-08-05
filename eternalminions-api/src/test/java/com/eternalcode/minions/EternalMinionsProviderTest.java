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
        public com.eternalcode.minions.minion.MinionManagementService minionManagementService() {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.eternalcode.minions.behavior.MinionBehaviorService minionBehaviorService() {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.eternalcode.minions.item.MinionItemService minionItemService() {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.eternalcode.minions.status.MinionStatusService minionStatusService() {
            throw new UnsupportedOperationException();
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
        public Optional<com.eternalcode.minions.minion.MinionSnapshot> findSnapshotById(
            com.eternalcode.minions.minion.MinionId minionId
        ) {
            return Optional.empty();
        }

        @Override
        public Collection<com.eternalcode.minions.minion.MinionDetails> findAll() {
            return List.of();
        }

        @Override
        public Collection<com.eternalcode.minions.minion.MinionSnapshot> findAllSnapshots() {
            return List.of();
        }

        @Override
        public Collection<com.eternalcode.minions.minion.MinionDetails> findByOwner(UUID ownerId) {
            return List.of();
        }

        @Override
        public int countByOwner(UUID ownerId) {
            return 0;
        }

        @Override
        public Optional<com.eternalcode.minions.minion.MinionDetails> findAt(
            com.eternalcode.minions.minion.MinionPosition position
        ) {
            return Optional.empty();
        }
    }
}
