package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.AddExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface AddExpenseItemUseCase {

    ExpenseItemId addItem(AddExpenseItemCommand command);
}
