package io.github.xfoundries.demo.payment.application;

import java.math.BigDecimal;
import java.util.Objects;

public final class PaymentRule {

    private static final String LIMIT_EXCEEDED = "SINGLE_PAYMENT_LIMIT_EXCEEDED";

    private final BigDecimal limit;
    private final String currency;

    public PaymentRule(BigDecimal limit, String currency) {
        this.limit = Objects.requireNonNull(limit);
        this.currency = Objects.requireNonNull(currency);
    }

    public PaymentDecision decide(BigDecimal amount, String paymentCurrency) {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(paymentCurrency);
        if (currency.equals(paymentCurrency) && amount.compareTo(limit) <= 0) {
            return PaymentDecision.paid();
        }
        return PaymentDecision.failed(
                LIMIT_EXCEEDED,
                "Approved amount exceeds the single-payment limit of %s %s"
                        .formatted(limit.toPlainString(), currency));
    }
}
