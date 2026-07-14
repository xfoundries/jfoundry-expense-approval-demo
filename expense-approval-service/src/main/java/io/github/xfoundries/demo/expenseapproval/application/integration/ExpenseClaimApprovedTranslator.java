package io.github.xfoundries.demo.expenseapproval.application.integration;

import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ExpenseClaimApprovedV1;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimApproved;

public final class ExpenseClaimApprovedTranslator {

    public EventEnvelope<ExpenseClaimApprovedV1> translate(ExpenseClaimApproved event) {
        String eventId = event.getEventId().toString();
        return EventEnvelope.create(
                eventId,
                "ExpenseClaimApproved",
                1,
                event.approvedAt(),
                eventId,
                null,
                event.claimId().value(),
                new ExpenseClaimApprovedV1(
                        event.claimId().value(),
                        event.claimantId().value(),
                        event.amount().amount(),
                        "CNY",
                        event.approvedAt()));
    }
}
