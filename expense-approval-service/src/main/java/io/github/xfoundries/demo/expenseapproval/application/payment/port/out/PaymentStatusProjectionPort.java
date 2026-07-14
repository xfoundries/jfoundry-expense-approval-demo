package io.github.xfoundries.demo.expenseapproval.application.payment.port.out;

import java.time.Instant;

import org.jfoundry.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface PaymentStatusProjectionPort {

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
