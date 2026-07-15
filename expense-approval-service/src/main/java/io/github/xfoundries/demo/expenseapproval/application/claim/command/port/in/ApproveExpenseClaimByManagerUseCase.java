package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByManagerCommand;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface ApproveExpenseClaimByManagerUseCase {

    void approveByManager(ApproveExpenseClaimByManagerCommand command);
}
