package io.github.xfoundries.demo.expenseapproval.application.payment;

import java.time.Instant;

public interface PaymentStatusProjectionStore {

    void upsert(PaymentStatusProjection projection);

    record PaymentStatusProjection(
            String claimId,
            String status,
            String paymentReference,
            String failureCode,
            String failureReason,
            Instant processedAt,
            String sourceEventId) {
    }
}
