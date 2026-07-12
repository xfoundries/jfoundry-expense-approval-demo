package io.github.xfoundries.demo.expenseapproval.application.service;

import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.Actor;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.ActorRole;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ExpenseClaimQueryUseCase;
import io.github.xfoundries.demo.expenseapproval.application.port.out.ExpenseClaimQueryPort;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import org.jfoundry.application.ApplicationService;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;

@ApplicationService
public final class ExpenseClaimQueryService implements ExpenseClaimQueryUseCase {

    private final ExpenseClaimQueryPort queryPort;

    public ExpenseClaimQueryService(ExpenseClaimQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public PageResult<ClaimSummary> findMine(Actor actor, ClaimState state, PageQuery page) {
        requireRole(actor, ActorRole.EMPLOYEE);
        validatePage(page);
        return queryPort.findOwned(actor.userId(), state, page);
    }

    @Override
    public PageResult<ClaimSummary> findManagerQueue(Actor actor, PageQuery page) {
        requireRole(actor, ActorRole.MANAGER);
        validatePage(page);
        return queryPort.findPendingManager(page);
    }

    @Override
    public PageResult<ClaimSummary> findFinanceQueue(Actor actor, PageQuery page) {
        requireRole(actor, ActorRole.FINANCE);
        validatePage(page);
        return queryPort.findPendingFinance(page);
    }

    @Override
    public ClaimDetail getDetail(Actor actor, ExpenseClaimId id) {
        ClaimDetail detail = queryPort.findDetail(id)
                .orElseThrow(() -> new NotFoundException("Expense claim not found: " + id.value()));
        boolean authorized = switch (actor.role()) {
            case EMPLOYEE -> detail.claimantId().equals(actor.userId().value());
            case MANAGER -> detail.state() == ClaimState.PENDING_MANAGER;
            case FINANCE -> detail.state() == ClaimState.PENDING_FINANCE;
        };
        if (!authorized) {
            throw new ClaimAccessDeniedException("Actor cannot view this expense claim");
        }
        return detail;
    }

    private static void validatePage(PageQuery page) {
        if (page == null || page.page() < 0 || page.size() < 1 || page.size() > 100) {
            throw new InvalidArgumentException("Page must be >= 0 and size must be between 1 and 100");
        }
    }

    private static void requireRole(Actor actor, ActorRole requiredRole) {
        if (actor.role() != requiredRole) {
            throw new InvalidArgumentException("This query requires role " + requiredRole);
        }
    }
}

