package io.github.xfoundries.demo.expenseapproval.application.port.out;

import java.util.Optional;

import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;

public interface ExpenseClaimQueryPort {

    PageResult<ClaimSummary> findOwned(UserId claimant, ClaimState state, PageQuery page);

    PageResult<ClaimSummary> findPendingManager(PageQuery page);

    PageResult<ClaimSummary> findPendingFinance(PageQuery page);

    Optional<ClaimDetail> findDetail(ExpenseClaimId id);
}

