package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RemoveExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class RemoveExpenseItemCommandHandler {

    private final ClaimCommandContext context;

    public RemoveExpenseItemCommandHandler(ClaimCommandContext context) {
        this.context = context;
    }

    @CommandHandler
    @ApplicationTransactional
    public void handle(RemoveExpenseItemCommand command) {
        context.requireRole(command.actor(), ApprovalRole.EMPLOYEE);
        ExpenseClaim claim = context.load(command.claimId());
        claim.removeItem(command.actor().userId(), command.itemId(), context.now());
        context.save(claim);
    }
}
