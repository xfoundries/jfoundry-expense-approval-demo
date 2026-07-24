package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RemoveExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class RemoveExpenseItemCommandHandler {

    private final ExpenseClaimCommandSupport support;

    public RemoveExpenseItemCommandHandler(ExpenseClaimCommandSupport support) {
        this.support = support;
    }

    @CommandHandler
    public void removeItem(RemoveExpenseItemCommand command) {
        support.inTransaction(() -> {
            support.requireRole(command.actor(), ApprovalRole.EMPLOYEE);
            ExpenseClaim claim = support.load(command.claimId());
            claim.removeItem(command.actor().userId(), command.itemId(), support.now());
            support.save(claim);
            return null;
        });
    }
}
