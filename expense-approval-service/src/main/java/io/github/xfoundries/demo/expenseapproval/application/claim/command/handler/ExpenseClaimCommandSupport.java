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
import org.jfoundry.application.exception.ExternalAccessException;
import org.jfoundry.application.transaction.TransactionCallback;
import org.jfoundry.application.transaction.TransactionRunner;

public final class ExpenseClaimCommandSupport {

    private final ExpenseClaimRepository repository;
    private final Clock clock;
    private final TransactionRunner transactions;

    public ExpenseClaimCommandSupport(
            ExpenseClaimRepository repository, Clock clock, TransactionRunner transactions) {
        this.repository = repository;
        this.clock = clock;
        this.transactions = transactions;
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

    <T> T inTransaction(TransactionCallback<T> callback) {
        try {
            return transactions.call(callback);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ExternalAccessException("Expense claim transaction failed", exception);
        }
    }
}
