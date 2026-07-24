package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.AddExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class AddExpenseItemCommandHandler {

    private final ExpenseClaimCommandSupport support;

    public AddExpenseItemCommandHandler(ExpenseClaimCommandSupport support) {
        this.support = support;
    }

    @CommandHandler
    public ExpenseItemId addItem(AddExpenseItemCommand command) {
        return support.inTransaction(() -> {
            support.requireRole(command.actor(), ApprovalRole.EMPLOYEE);
            ExpenseClaim claim = support.load(command.claimId());
            ExpenseItem item = new ExpenseItem(
                    ExpenseItemId.generate(),
                    command.expenseDate(),
                    command.category(),
                    Money.positiveCny(command.amount()),
                    command.description(),
                    command.receiptReference());
            claim.addItem(command.actor().userId(), item, support.now());
            support.save(claim);
            return item.id();
        });
    }
}
