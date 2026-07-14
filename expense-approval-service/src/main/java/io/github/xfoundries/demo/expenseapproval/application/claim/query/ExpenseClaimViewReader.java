package io.github.xfoundries.demo.expenseapproval.application.claim.query;

import java.util.Optional;

import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;

public interface ExpenseClaimViewReader {

    PageResult<ClaimSummary> findOwned(UserId claimant, ClaimState state, PageQuery page);

    PageResult<ClaimSummary> findPendingManager(PageQuery page);

    PageResult<ClaimSummary> findPendingFinance(PageQuery page);

    Optional<ClaimDetail> findDetail(ExpenseClaimId id);
}
