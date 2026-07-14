package io.github.xfoundries.demo.expenseapproval.domain.policy;

import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonthlyExpenseLimitPolicyTest {

    private final MonthlyExpenseLimitPolicy policy =
            new MonthlyExpenseLimitPolicy(Money.cny("10000.00"));

    @Test
    void allowsTheExactMonthlyLimit() {
        assertThatCode(() -> policy.ensureWithinLimit(
                Money.cny("8000.00"), Money.cny("2000.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnAmountAboveTheMonthlyLimit() {
        assertThatThrownBy(() -> policy.ensureWithinLimit(
                Money.cny("8000.00"), Money.cny("2000.01")))
                .isInstanceOf(MonthlyExpenseLimitExceeded.class)
                .hasMessageContaining("10000.00");
    }
}
