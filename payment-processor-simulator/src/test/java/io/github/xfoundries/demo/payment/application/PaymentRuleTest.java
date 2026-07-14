package io.github.xfoundries.demo.payment.application;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRuleTest {

    private final PaymentRule rule = new PaymentRule(new BigDecimal("8000.00"), "CNY");

    @Test
    void paysAnAmountAtTheConfiguredLimit() {
        PaymentDecision decision = rule.decide(new BigDecimal("8000.00"), "CNY");

        assertThat(decision.successful()).isTrue();
        assertThat(decision.failureCode()).isNull();
        assertThat(decision.failureReason()).isNull();
    }

    @Test
    void rejectsAnAmountAboveTheConfiguredLimit() {
        PaymentDecision decision = rule.decide(new BigDecimal("8000.01"), "CNY");

        assertThat(decision.successful()).isFalse();
        assertThat(decision.failureCode()).isEqualTo("SINGLE_PAYMENT_LIMIT_EXCEEDED");
        assertThat(decision.failureReason())
                .isEqualTo("Approved amount exceeds the single-payment limit of 8000.00 CNY");
    }
}
