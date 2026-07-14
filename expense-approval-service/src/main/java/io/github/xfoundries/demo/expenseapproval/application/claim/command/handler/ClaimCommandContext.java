package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import java.time.Clock;
import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;

public final class ClaimCommandContext {

    private final ExpenseClaimRepository repository;
    private final Clock clock;

    public ClaimCommandContext(ExpenseClaimRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    ExpenseClaim load(ExpenseClaimId id) {
        ExpenseClaim claim = repository.findById(id);
        if (claim == null) {
            throw new NotFoundException("Expense claim not found: " + id.value());
        }
        return claim;
    }

    void requireRole(ApprovalActor actor, ApprovalRole requiredRole) {
        if (actor.role() != requiredRole) {
            throw new InvalidArgumentException("This action requires role " + requiredRole);
        }
    }

    Instant now() {
        return clock.instant();
    }

    void save(ExpenseClaim claim) {
        repository.modify(claim);
    }
}
