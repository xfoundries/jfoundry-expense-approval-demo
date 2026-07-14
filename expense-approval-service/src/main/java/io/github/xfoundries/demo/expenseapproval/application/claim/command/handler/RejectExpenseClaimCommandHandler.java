package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RejectExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.RejectionReason;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class RejectExpenseClaimCommandHandler {

    private final ClaimCommandContext context;

    public RejectExpenseClaimCommandHandler(ClaimCommandContext context) {
        this.context = context;
    }

    @CommandHandler
    @ApplicationTransactional
    public void handle(RejectExpenseClaimCommand command) {
        ExpenseClaim claim = context.load(command.claimId());
        ApprovalRole requiredRole = requiredRole(claim.state());
        context.requireRole(command.actor(), requiredRole);
        claim.reject(
                command.actor().userId(), RejectionReason.of(command.reason()), context.now());
        context.save(claim);
    }

    private static ApprovalRole requiredRole(ClaimState state) {
        return switch (state) {
            case PENDING_MANAGER -> ApprovalRole.MANAGER;
            case PENDING_FINANCE -> ApprovalRole.FINANCE;
            default -> throw new InvalidArgumentException("Expense claim is not waiting for approval");
        };
    }
}
