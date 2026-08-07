package com.eternalcode.minions.minion.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.eternalcode.minions.bridge.vault.EconomyService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpgradePaymentTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void shouldReturnFalseWhenNoEconomyServiceIsRegistered() {
        UpgradePayment payment = new UpgradePayment(Optional.empty());

        assertThat(payment.withdraw(PLAYER_ID, BigDecimal.TEN)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenRegisteredEconomyServiceIsUnavailable() {
        FakeEconomyService economy = new FakeEconomyService(false);
        UpgradePayment payment = new UpgradePayment(Optional.of(economy));

        assertThat(payment.withdraw(PLAYER_ID, BigDecimal.TEN)).isFalse();
        assertThat(economy.balanceOf(PLAYER_ID)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldWithdrawFromPlayerBalanceWhenFundsAreSufficient() {
        FakeEconomyService economy = new FakeEconomyService(true);
        economy.deposit(PLAYER_ID, new BigDecimal("50.00"));
        UpgradePayment payment = new UpgradePayment(Optional.of(economy));

        boolean withdrawn = payment.withdraw(PLAYER_ID, new BigDecimal("20.00"));

        assertThat(withdrawn).isTrue();
        assertThat(economy.balanceOf(PLAYER_ID)).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void shouldNotWithdrawWhenPlayerHasInsufficientFunds() {
        FakeEconomyService economy = new FakeEconomyService(true);
        economy.deposit(PLAYER_ID, new BigDecimal("5.00"));
        UpgradePayment payment = new UpgradePayment(Optional.of(economy));

        boolean withdrawn = payment.withdraw(PLAYER_ID, new BigDecimal("20.00"));

        assertThat(withdrawn).isFalse();
        assertThat(economy.balanceOf(PLAYER_ID))
                .as("a failed withdrawal must not touch the player's balance")
                .isEqualByComparingTo("5.00");
    }

    @Test
    void shouldRejectZeroOrNegativeWithdrawAmount() {
        UpgradePayment payment = new UpgradePayment(Optional.of(new FakeEconomyService(true)));

        assertThatIllegalArgumentException().isThrownBy(() -> payment.withdraw(PLAYER_ID, BigDecimal.ZERO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> payment.withdraw(PLAYER_ID, new BigDecimal("-1.00")));
    }

    @Test
    void shouldFallBackToPlainStringWhenNoEconomyServiceIsAvailableForFormatting() {
        UpgradePayment payment = new UpgradePayment(Optional.empty());

        assertThat(payment.format(new BigDecimal("12.50"))).isEqualTo("12.50");
    }

    @Test
    void shouldDelegateFormattingToEconomyServiceWhenAvailable() {
        FakeEconomyService economy = new FakeEconomyService(true);
        UpgradePayment payment = new UpgradePayment(Optional.of(economy));

        assertThat(payment.format(new BigDecimal("12.50"))).isEqualTo("$12.50");
    }

    private static final class FakeEconomyService implements EconomyService {

        private final boolean available;
        private final Map<UUID, BigDecimal> balances = new HashMap<>();

        private FakeEconomyService(boolean available) {
            this.available = available;
        }

        void deposit(UUID playerId, BigDecimal amount) {
            this.balances.merge(playerId, amount, BigDecimal::add);
        }

        BigDecimal balanceOf(UUID playerId) {
            return this.balances.getOrDefault(playerId, BigDecimal.ZERO);
        }

        @Override
        public boolean available() {
            return this.available;
        }

        @Override
        public boolean withdraw(UUID playerId, BigDecimal amount) {
            BigDecimal balance = this.balanceOf(playerId);
            if (balance.compareTo(amount) < 0) {
                return false;
            }
            this.balances.put(playerId, balance.subtract(amount));
            return true;
        }

        @Override
        public String format(BigDecimal amount) {
            return "$" + amount.toPlainString();
        }
    }
}
