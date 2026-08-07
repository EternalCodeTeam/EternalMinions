package com.eternalcode.minions.minion.limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionEquipment;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionProgress;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.Test;

class PlayerMinionLimitServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final MinionRegistry registry = new MinionRegistry();
    private final MinionsConfig.LimitsConfig config = new MinionsConfig.LimitsConfig();
    private final PlayerMinionLimitService limits = new PlayerMinionLimitService(this.registry, this.config);

    @Test
    void shouldFallBackToConfiguredDefaultWhenPlayerHasNoLimitPermission() {
        this.config.defaultLimit = 5;
        Player player = playerWithPermissions(OWNER_ID, false, List.of());

        MinionLimitStatus status = this.limits.statusFor(player);

        assertThat(status.max()).isEqualTo(5);
        assertThat(status.unlimited()).isFalse();
    }

    @Test
    void shouldUseHighestGrantedLimitPermissionAmongSeveral() {
        Player player = playerWithPermissions(OWNER_ID, false, List.of(
                grantedPermission("eternalminions.limit.10"),
                grantedPermission("eternalminions.limit.30"),
                grantedPermission("eternalminions.limit.20")
        ));

        MinionLimitStatus status = this.limits.statusFor(player);

        assertThat(status.max()).isEqualTo(30);
    }

    @Test
    void shouldIgnorePermissionsThatAreNotActuallyGranted() {
        Player player = playerWithPermissions(OWNER_ID, false, List.of(
                deniedPermission("eternalminions.limit.100"),
                grantedPermission("eternalminions.limit.10")
        ));

        MinionLimitStatus status = this.limits.statusFor(player);

        assertThat(status.max()).isEqualTo(10);
    }

    @Test
    void shouldIgnoreLimitPermissionsWithANonNumericSuffix() {
        this.config.defaultLimit = 5;
        Player player = playerWithPermissions(OWNER_ID, false, List.of(
                grantedPermission("eternalminions.limit.unlimited-ish")
        ));

        MinionLimitStatus status = this.limits.statusFor(player);

        assertThat(status.max()).isEqualTo(5);
    }

    @Test
    void shouldTreatWildcardPermissionAsTrulyUnlimitedRegardlessOfNumericGrants() {
        Player player = playerWithPermissions(OWNER_ID, true, List.of(
                grantedPermission("eternalminions.limit.5")
        ));

        MinionLimitStatus status = this.limits.statusFor(player);

        assertThat(status.unlimited()).isTrue();
        assertThat(status.max()).isEqualTo(Integer.MAX_VALUE);
        assertThat(status.reached()).isFalse();
    }

    @Test
    void shouldClampNegativeDefaultLimitToZeroInsteadOfAllowingNegativeCapacity() {
        this.config.defaultLimit = -3;
        Player player = playerWithPermissions(OWNER_ID, false, List.of());

        MinionLimitStatus status = this.limits.statusFor(player);

        assertThat(status.max()).isZero();
        assertThat(status.reached()).isTrue();
    }

    @Test
    void shouldCountOnlyMinionsOwnedByTheQueriedPlayer() {
        UUID otherOwner = UUID.fromString("00000000-0000-0000-0000-000000000002");
        this.registry.register(minionOwnedBy(1, OWNER_ID));
        this.registry.register(minionOwnedBy(2, OWNER_ID));
        this.registry.register(minionOwnedBy(3, otherOwner));
        this.config.defaultLimit = 5;
        Player player = playerWithPermissions(OWNER_ID, false, List.of());

        MinionLimitStatus status = this.limits.statusFor(player);

        assertThat(status.current()).isEqualTo(2);
        assertThat(status.reached()).isFalse();
    }

    @Test
    void shouldReportLimitReachedWhenCurrentCountEqualsMax() {
        this.config.defaultLimit = 1;
        this.registry.register(minionOwnedBy(1, OWNER_ID));
        Player player = playerWithPermissions(OWNER_ID, false, List.of());

        MinionLimitStatus status = this.limits.statusFor(player);

        assertThat(status.current()).isEqualTo(1);
        assertThat(status.max()).isEqualTo(1);
        assertThat(status.reached()).isTrue();
    }

    private static PermissionAttachmentInfo grantedPermission(String permission) {
        return permissionInfo(permission, true);
    }

    private static PermissionAttachmentInfo deniedPermission(String permission) {
        return permissionInfo(permission, false);
    }

    private static PermissionAttachmentInfo permissionInfo(String permission, boolean value) {
        Player dummyPermissible = mock(Player.class);
        return new PermissionAttachmentInfo(dummyPermissible, permission, null, value);
    }

    private static Player playerWithPermissions(UUID id, boolean unlimited, List<PermissionAttachmentInfo> permissions) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.hasPermission("eternalminions.limit.*")).thenReturn(unlimited);
        when(player.getEffectivePermissions()).thenReturn(new java.util.HashSet<>(permissions));
        return player;
    }

    private static Minion minionOwnedBy(long id, UUID ownerId) {
        return new Minion(
                new MinionId(id),
                ownerId,
                "MINER",
                new MinionPosition("world", (int) id, 64, 0),
                MinionProgress.start(),
                MinionEquipment.empty(),
                new MinionStorage(9),
                MinionUpgrades.none(),
                null,
                MinionSettings.defaults()
        );
    }
}
