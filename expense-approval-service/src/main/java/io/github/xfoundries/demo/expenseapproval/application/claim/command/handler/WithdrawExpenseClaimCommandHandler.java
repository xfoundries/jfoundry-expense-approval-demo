package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.WithdrawExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class WithdrawExpenseClaimCommandHandler {

    private final ExpenseClaimCommandSupport support;

    public WithdrawExpenseClaimCommandHandler(ExpenseClaimCommandSupport support) {
        this.support = support;
    }

    @CommandHandler
    @ApplicationTransactional
    public void withdraw(WithdrawExpenseClaimCommand command) {
        support.requireRole(command.actor(), ApprovalRole.EMPLOYEE);
        ExpenseClaim claim = support.load(command.claimId());
        claim.withdraw(command.actor().userId(), support.now());
        support.save(claim);
    }
}
