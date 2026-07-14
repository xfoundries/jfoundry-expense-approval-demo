package io.github.xfoundries.demo.expenseapproval.application.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.command.ActorRole;
import io.github.xfoundries.demo.expenseapproval.application.command.SubmitExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class SubmitExpenseClaimCommandHandler {

    private final ClaimCommandContext context;

    public SubmitExpenseClaimCommandHandler(ClaimCommandContext context) {
        this.context = context;
    }

    @CommandHandler
    @ApplicationTransactional
    public void handle(SubmitExpenseClaimCommand command) {
        context.requireRole(command.actor(), ActorRole.EMPLOYEE);
        ExpenseClaim claim = context.load(command.claimId());
        claim.submit(command.actor().userId(), context.now());
        context.save(claim);
    }
}
