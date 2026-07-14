package io.github.xfoundries.demo.expenseapproval.application.command;

import java.util.Objects;

import io.github.xfoundries.demo.expenseapproval.domain.model.Money;

public final class MonthlyApprovalLimitPolicy {

    private final Money limit;

    public MonthlyApprovalLimitPolicy(Money limit) {
        this.limit = Objects.requireNonNull(limit, "limit must not be null");
    }

    public void ensureWithinLimit(Money approvedAmount, Money candidateAmount) {
        Objects.requireNonNull(approvedAmount, "approvedAmount must not be null");
        Objects.requireNonNull(candidateAmount, "candidateAmount must not be null");
        if (approvedAmount.add(candidateAmount).isGreaterThan(limit)) {
            throw new MonthlyExpenseLimitExceeded(
                    "Monthly approved expense limit exceeded: " + limit.amount());
        }
    }
}
