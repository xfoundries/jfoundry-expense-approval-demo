package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.AddExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class AddExpenseItemCommandHandler {

    private final ClaimCommandContext context;

    public AddExpenseItemCommandHandler(ClaimCommandContext context) {
        this.context = context;
    }

    @CommandHandler
    @ApplicationTransactional
    public ExpenseItemId handle(AddExpenseItemCommand command) {
        context.requireRole(command.actor(), ApprovalRole.EMPLOYEE);
        ExpenseClaim claim = context.load(command.claimId());
        ExpenseItem item = new ExpenseItem(
                ExpenseItemId.generate(),
                command.expenseDate(),
                command.category(),
                Money.positiveCny(command.amount()),
                command.description(),
                command.receiptReference());
        claim.addItem(command.actor().userId(), item, context.now());
        context.save(claim);
        return item.id();
    }
}
