package io.github.xfoundries.demo.expenseapproval.application.claim.query.port.out;

import java.util.Optional;

import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import org.jfoundry.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface ExpenseClaimViewPort {

    PageResult<ClaimSummary> findOwned(UserId claimant, ClaimState state, PageQuery page);

    PageResult<ClaimSummary> findPendingManager(PageQuery page);

    PageResult<ClaimSummary> findPendingFinance(PageQuery page);

    Optional<ClaimDetail> findDetail(ExpenseClaimId id);
}
