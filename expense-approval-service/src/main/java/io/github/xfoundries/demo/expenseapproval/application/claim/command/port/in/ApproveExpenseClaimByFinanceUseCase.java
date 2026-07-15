package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByFinanceCommand;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface ApproveExpenseClaimByFinanceUseCase {

    void approveByFinance(ApproveExpenseClaimByFinanceCommand command);
}
