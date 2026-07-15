package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.ReopenExpenseClaimCommand;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface ReopenExpenseClaimUseCase {

    void reopen(ReopenExpenseClaimCommand command);
}
