package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.CreateExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface CreateExpenseClaimUseCase {

    ExpenseClaimId create(CreateExpenseClaimCommand command);
}
