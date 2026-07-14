package io.github.xfoundries.demo.expenseapproval.application.claim.query;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;

public interface ExpenseClaimQueries {

    PageResult<ClaimSummary> findMine(ApprovalActor actor, ClaimState state, PageQuery page);

    PageResult<ClaimSummary> findManagerQueue(ApprovalActor actor, PageQuery page);

    PageResult<ClaimSummary> findFinanceQueue(ApprovalActor actor, PageQuery page);

    ClaimDetail getDetail(ApprovalActor actor, ExpenseClaimId id);
}
