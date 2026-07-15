package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.UpdateExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class UpdateExpenseItemCommandHandler {

    private final ExpenseClaimCommandSupport support;

    public UpdateExpenseItemCommandHandler(ExpenseClaimCommandSupport support) {
        this.support = support;
    }

    @CommandHandler
    @ApplicationTransactional
    public void updateItem(UpdateExpenseItemCommand command) {
        support.requireRole(command.actor(), ApprovalRole.EMPLOYEE);
        ExpenseClaim claim = support.load(command.claimId());
        ExpenseItem item = new ExpenseItem(
                command.itemId(),
                command.expenseDate(),
                command.category(),
                Money.positiveCny(command.amount()),
                command.description(),
                command.receiptReference());
        claim.updateItem(command.actor().userId(), item, support.now());
        support.save(claim);
    }
}
