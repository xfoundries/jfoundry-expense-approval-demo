package io.github.xfoundries.demo.contracts;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseClaimApprovedV1(
        String claimId,
        String employeeId,
        BigDecimal approvedAmount,
        String currency,
        Instant approvedAt) {
}
