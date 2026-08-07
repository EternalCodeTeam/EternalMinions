package com.eternalcode.minions.minion.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.access.MinionAccessRegistration;
import com.eternalcode.minions.minion.MinionDetails;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinionAccessServiceImplTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final MinionDetails minion = new MinionDetails(
            new MinionId(1),
            OWNER_ID,
            "MINER",
            new MinionPosition("world", 0, 64, 0),
            1
    );

    private MinionAccessServiceImpl access;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        this.access = new MinionAccessServiceImpl(Logger.getAnonymousLogger());
        this.plugin = mock(Plugin.class);
        when(this.plugin.isEnabled()).thenReturn(true);
    }

    @Test
    void shouldAlwaysAllowOwnerRegardlessOfRegisteredPolicies() {
        Player owner = playerWithId(OWNER_ID);
        this.access.registerPolicy(this.plugin, (player, details, action) -> false);

        assertThat(this.access.canAccess(owner, this.minion, MinionAccessAction.MANAGE)).isTrue();
    }

    @Test
    void shouldDenyNonOwnerWhenNoPolicyIsRegistered() {
        Player stranger = playerWithId(OTHER_PLAYER_ID);

        assertThat(this.access.canAccess(stranger, this.minion, MinionAccessAction.MANAGE)).isFalse();
    }

    @Test
    void shouldAllowNonOwnerWhenARegisteredPolicyGrantsAccess() {
        Player trustedFriend = playerWithId(OTHER_PLAYER_ID);
        this.access.registerPolicy(this.plugin, (player, details, action) -> true);

        assertThat(this.access.canAccess(trustedFriend, this.minion, MinionAccessAction.MANAGE)).isTrue();
    }

    @Test
    void shouldTreatAThrowingPolicyAsDeniedInsteadOfPropagating() {
        Player stranger = playerWithId(OTHER_PLAYER_ID);
        this.access.registerPolicy(this.plugin, (player, details, action) -> {
            throw new IllegalStateException("broken integration");
        });

        assertThat(this.access.canAccess(stranger, this.minion, MinionAccessAction.MANAGE)).isFalse();
    }

    @Test
    void shouldStopConsultingAPolicyAfterItIsUnregistered() {
        Player stranger = playerWithId(OTHER_PLAYER_ID);
        MinionAccessRegistration registration =
                this.access.registerPolicy(this.plugin, (player, details, action) -> true);

        registration.unregister();

        assertThat(registration.isRegistered()).isFalse();
        assertThat(this.access.canAccess(stranger, this.minion, MinionAccessAction.MANAGE)).isFalse();
    }

    @Test
    void shouldRejectRegisteringAPolicyForAnAlreadyDisabledPlugin() {
        when(this.plugin.isEnabled()).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatIllegalStateException()
                .isThrownBy(() -> this.access.registerPolicy(this.plugin, (player, details, action) -> true));
    }

    @Test
    void shouldRemovePolicyWhenItsOwningPluginIsDisabledViaEvent() {
        Player stranger = playerWithId(OTHER_PLAYER_ID);
        MinionAccessRegistration registration =
                this.access.registerPolicy(this.plugin, (player, details, action) -> true);
        assertThat(this.access.canAccess(stranger, this.minion, MinionAccessAction.MANAGE)).isTrue();

        this.access.onPluginDisable(new PluginDisableEvent(this.plugin));

        assertThat(registration.isRegistered()).isFalse();
        assertThat(this.access.canAccess(stranger, this.minion, MinionAccessAction.MANAGE)).isFalse();
    }

    @Test
    void shouldLazilyUnregisterAPolicyFoundDisabledWithoutHavingReceivedTheDisableEvent() {
        Player stranger = playerWithId(OTHER_PLAYER_ID);
        MinionAccessRegistration registration =
                this.access.registerPolicy(this.plugin, (player, details, action) -> true);

        // Simulates the plugin having been disabled through a path that never fired
        // PluginDisableEvent through this listener (e.g. it was already unregistered elsewhere).
        when(this.plugin.isEnabled()).thenReturn(false);

        assertThat(this.access.canAccess(stranger, this.minion, MinionAccessAction.MANAGE)).isFalse();
        assertThat(registration.isRegistered())
                .as("a stale registration for a disabled plugin must be pruned on use")
                .isFalse();
    }

    private static Player playerWithId(UUID id) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        return player;
    }
}
