package io.github.xfoundries.demo.expenseapproval.application.claim.query;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.port.in.ExpenseClaimQueryUseCase;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.port.out.ExpenseClaimViewPort;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import org.jfoundry.application.ApplicationService;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;

@ApplicationService
public class ExpenseClaimQueryService implements ExpenseClaimQueryUseCase {

    private final ExpenseClaimViewPort viewPort;

    public ExpenseClaimQueryService(ExpenseClaimViewPort viewPort) {
        this.viewPort = viewPort;
    }

    @Override
    public PageResult<ClaimSummary> findMine(ApprovalActor actor, ClaimState state, PageQuery page) {
        requireRole(actor, ApprovalRole.EMPLOYEE);
        validatePage(page);
        return viewPort.findOwned(actor.userId(), state, page);
    }

    @Override
    public PageResult<ClaimSummary> findManagerQueue(ApprovalActor actor, PageQuery page) {
        requireRole(actor, ApprovalRole.MANAGER);
        validatePage(page);
        return viewPort.findPendingManager(page);
    }

    @Override
    public PageResult<ClaimSummary> findFinanceQueue(ApprovalActor actor, PageQuery page) {
        requireRole(actor, ApprovalRole.FINANCE);
        validatePage(page);
        return viewPort.findPendingFinance(page);
    }

    @Override
    public ClaimDetail getDetail(ApprovalActor actor, ExpenseClaimId id) {
        ClaimDetail detail = viewPort.findDetail(id)
                .orElseThrow(() -> new NotFoundException("Expense claim not found: " + id.value()));
        boolean authorized = switch (actor.role()) {
            case EMPLOYEE -> detail.claimantId().equals(actor.userId().value());
            case MANAGER -> detail.state() == ClaimState.PENDING_MANAGER;
            case FINANCE -> detail.state() == ClaimState.PENDING_FINANCE;
        };
        if (!authorized) {
            throw new ClaimAccessDeniedException("ApprovalActor cannot view this expense claim");
        }
        return detail;
    }

    private static void validatePage(PageQuery page) {
        if (page == null || page.page() < 0 || page.size() < 1 || page.size() > 100) {
            throw new InvalidArgumentException("Page must be >= 0 and size must be between 1 and 100");
        }
    }

    private static void requireRole(ApprovalActor actor, ApprovalRole requiredRole) {
        if (actor.role() != requiredRole) {
            throw new InvalidArgumentException("This query requires role " + requiredRole);
        }
    }
}
