package io.github.xfoundries.demo.expenseapproval.application.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.command.ActorRole;
import io.github.xfoundries.demo.expenseapproval.application.command.RejectExpenseClaimCommand;
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
        ActorRole requiredRole = requiredRole(claim.state());
        context.requireRole(command.actor(), requiredRole);
        claim.reject(
                command.actor().userId(), RejectionReason.of(command.reason()), context.now());
        context.save(claim);
    }

    private static ActorRole requiredRole(ClaimState state) {
        return switch (state) {
            case PENDING_MANAGER -> ActorRole.MANAGER;
            case PENDING_FINANCE -> ActorRole.FINANCE;
            default -> throw new InvalidArgumentException("Expense claim is not waiting for approval");
        };
    }
}
