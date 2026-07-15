package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.RejectExpenseClaimCommand;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface RejectExpenseClaimUseCase {

    void reject(RejectExpenseClaimCommand command);
}
