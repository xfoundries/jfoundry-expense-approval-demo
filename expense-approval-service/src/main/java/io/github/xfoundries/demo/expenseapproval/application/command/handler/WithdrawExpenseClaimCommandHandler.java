package io.github.xfoundries.demo.expenseapproval.application.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.command.ActorRole;
import io.github.xfoundries.demo.expenseapproval.application.command.WithdrawExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class WithdrawExpenseClaimCommandHandler {

    private final ClaimCommandContext context;

    public WithdrawExpenseClaimCommandHandler(ClaimCommandContext context) {
        this.context = context;
    }

    @CommandHandler
    @ApplicationTransactional
    public void handle(WithdrawExpenseClaimCommand command) {
        context.requireRole(command.actor(), ActorRole.EMPLOYEE);
        ExpenseClaim claim = context.load(command.claimId());
        claim.withdraw(command.actor().userId(), context.now());
        context.save(claim);
    }
}
