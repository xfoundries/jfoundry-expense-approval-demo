package io.github.xfoundries.demo.contracts;

import java.time.Instant;

public record ReimbursementPaidV1(
        String claimId,
        String paymentReference,
        Instant paidAt) {
}
