package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ReopenExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class ReopenExpenseClaimCommandHandler {

    private final ClaimCommandContext context;

    public ReopenExpenseClaimCommandHandler(ClaimCommandContext context) {
        this.context = context;
    }

    @CommandHandler
    @ApplicationTransactional
    public void handle(ReopenExpenseClaimCommand command) {
        context.requireRole(command.actor(), ApprovalRole.EMPLOYEE);
        ExpenseClaim claim = context.load(command.claimId());
        claim.reopen(command.actor().userId(), context.now());
        context.save(claim);
    }
}
