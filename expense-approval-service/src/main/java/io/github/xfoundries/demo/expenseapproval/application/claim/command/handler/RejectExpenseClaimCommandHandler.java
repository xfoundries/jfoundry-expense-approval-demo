package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RejectExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in.RejectExpenseClaimUseCase;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.RejectionReason;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class RejectExpenseClaimCommandHandler implements RejectExpenseClaimUseCase {

    private final ExpenseClaimCommandSupport support;

    public RejectExpenseClaimCommandHandler(ExpenseClaimCommandSupport support) {
        this.support = support;
    }

    @Override
    @CommandHandler
    @ApplicationTransactional
    public void reject(RejectExpenseClaimCommand command) {
        ExpenseClaim claim = support.load(command.claimId());
        ApprovalRole requiredRole = requiredRole(claim.state());
        support.requireRole(command.actor(), requiredRole);
        claim.reject(
                command.actor().userId(), RejectionReason.of(command.reason()), support.now());
        support.save(claim);
    }

    private static ApprovalRole requiredRole(ClaimState state) {
        return switch (state) {
            case PENDING_MANAGER -> ApprovalRole.MANAGER;
            case PENDING_FINANCE -> ApprovalRole.FINANCE;
            default -> throw new InvalidArgumentException("Expense claim is not waiting for approval");
        };
    }
}
