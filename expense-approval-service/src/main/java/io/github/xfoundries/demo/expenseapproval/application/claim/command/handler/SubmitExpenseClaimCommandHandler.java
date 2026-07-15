package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.SubmitExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in.SubmitExpenseClaimUseCase;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class SubmitExpenseClaimCommandHandler implements SubmitExpenseClaimUseCase {

    private final ExpenseClaimCommandSupport support;

    public SubmitExpenseClaimCommandHandler(ExpenseClaimCommandSupport support) {
        this.support = support;
    }

    @Override
    @CommandHandler
    @ApplicationTransactional
    public void submit(SubmitExpenseClaimCommand command) {
        support.requireRole(command.actor(), ApprovalRole.EMPLOYEE);
        ExpenseClaim claim = support.load(command.claimId());
        claim.submit(command.actor().userId(), support.now());
        support.save(claim);
    }
}
