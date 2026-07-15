package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.SubmitExpenseClaimCommand;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface SubmitExpenseClaimUseCase {

    void submit(SubmitExpenseClaimCommand command);
}
