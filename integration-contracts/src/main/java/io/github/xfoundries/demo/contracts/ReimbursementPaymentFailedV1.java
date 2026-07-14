package io.github.xfoundries.demo.contracts;

import java.time.Instant;

public record ReimbursementPaymentFailedV1(
        String claimId,
        String failureCode,
        String failureReason,
        Instant failedAt) {
}
