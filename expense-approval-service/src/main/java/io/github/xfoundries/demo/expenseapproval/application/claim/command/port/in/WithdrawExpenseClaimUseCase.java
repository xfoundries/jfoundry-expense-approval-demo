package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.WithdrawExpenseClaimCommand;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface WithdrawExpenseClaimUseCase {

    void withdraw(WithdrawExpenseClaimCommand command);
}
