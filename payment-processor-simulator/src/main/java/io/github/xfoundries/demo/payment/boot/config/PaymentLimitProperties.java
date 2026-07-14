package io.github.xfoundries.demo.payment.boot.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("payment-simulator.single-payment-limit")
public record PaymentLimitProperties(BigDecimal amount, String currency) {
}
