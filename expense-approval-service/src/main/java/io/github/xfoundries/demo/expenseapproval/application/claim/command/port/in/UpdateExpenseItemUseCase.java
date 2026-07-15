package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.UpdateExpenseItemCommand;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface UpdateExpenseItemUseCase {

    void updateItem(UpdateExpenseItemCommand command);
}
