package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.UpdateExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class UpdateExpenseItemCommandHandler {

    private final ClaimCommandContext context;

    public UpdateExpenseItemCommandHandler(ClaimCommandContext context) {
        this.context = context;
    }

    @CommandHandler
    @ApplicationTransactional
    public void handle(UpdateExpenseItemCommand command) {
        context.requireRole(command.actor(), ApprovalRole.EMPLOYEE);
        ExpenseClaim claim = context.load(command.claimId());
        ExpenseItem item = new ExpenseItem(
                command.itemId(),
                command.expenseDate(),
                command.category(),
                Money.positiveCny(command.amount()),
                command.description(),
                command.receiptReference());
        claim.updateItem(command.actor().userId(), item, context.now());
        context.save(claim);
    }
}
