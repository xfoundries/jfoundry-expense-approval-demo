package io.github.xfoundries.demo.expenseapproval.application.identity;

import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;

public record ApprovalActor(UserId userId, ApprovalRole role) {

    public ApprovalActor {
        if (userId == null || role == null) {
            throw new IllegalArgumentException("ApprovalActor user id and role are required");
        }
    }
}
