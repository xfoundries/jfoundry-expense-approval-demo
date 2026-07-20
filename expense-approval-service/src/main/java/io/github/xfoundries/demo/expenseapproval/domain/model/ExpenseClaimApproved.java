package io.github.xfoundries.demo.expenseapproval.domain.model;

import java.time.Instant;
import java.util.Objects;

import org.jfoundry.domain.event.BaseDomainEvent;

public final class ExpenseClaimApproved extends BaseDomainEvent {

    private final ExpenseClaimId claimId;
    private final UserId claimantId;
    private final Money amount;
    private final Instant approvedAt;

    public ExpenseClaimApproved(
            ExpenseClaimId claimId, UserId claimantId, Money amount, Instant approvedAt) {
        this.claimId = Objects.requireNonNull(claimId, "claimId must not be null");
        this.claimantId = Objects.requireNonNull(claimantId, "claimantId must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.approvedAt = Objects.requireNonNull(approvedAt, "approvedAt must not be null");
    }

    public ExpenseClaimId claimId() {
        return claimId;
    }

    public UserId claimantId() {
        return claimantId;
    }

    public Money amount() {
        return amount;
    }

    public Instant approvedAt() {
        return approvedAt;
    }
}
