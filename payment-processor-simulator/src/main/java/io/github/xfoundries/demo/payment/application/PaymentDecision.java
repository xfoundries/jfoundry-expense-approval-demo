package io.github.xfoundries.demo.payment.application;

public record PaymentDecision(
        boolean successful,
        String failureCode,
        String failureReason) {

    public static PaymentDecision paid() {
        return new PaymentDecision(true, null, null);
    }

    public static PaymentDecision failed(String code, String reason) {
        return new PaymentDecision(false, code, reason);
    }
}
