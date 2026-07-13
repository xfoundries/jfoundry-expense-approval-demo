package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;

final class ExpenseClaimRootDataMapper {

    public ExpenseClaimData toData(ExpenseClaim claim) {
        ExpenseClaimData data = new ExpenseClaimData();
        data.setId(claim.id().value());
        data.setClaimantId(claim.claimant().value());
        data.setTitle(claim.title());
        data.setState(claim.state().name());
        data.setTotalAmount(claim.total().amount());
        data.setFinanceApprovalRequired(claim.financeApprovalRequired());
        data.setCreatedAt(claim.createdAt());
        data.setUpdatedAt(claim.updatedAt());
        data.setSubmittedAt(claim.submittedAt().orElse(null));
        data.setCompletedAt(claim.completedAt().orElse(null));
        return data;
    }

    public String toDataId(ExpenseClaimId id) {
        return id == null ? null : id.value();
    }
}
