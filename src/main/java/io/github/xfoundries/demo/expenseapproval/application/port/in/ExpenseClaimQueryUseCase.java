package io.github.xfoundries.demo.expenseapproval.application.port.in;

import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.Actor;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;

public interface ExpenseClaimQueryUseCase {

    PageResult<ClaimSummary> findMine(Actor actor, ClaimState state, PageQuery page);

    PageResult<ClaimSummary> findManagerQueue(Actor actor, PageQuery page);

    PageResult<ClaimSummary> findFinanceQueue(Actor actor, PageQuery page);

    ClaimDetail getDetail(Actor actor, ExpenseClaimId id);
}

