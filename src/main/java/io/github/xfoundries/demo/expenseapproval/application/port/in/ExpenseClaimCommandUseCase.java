package io.github.xfoundries.demo.expenseapproval.application.port.in;

import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.AddItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.ApproveCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.ClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.CreateClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.RejectClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.RemoveItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.UpdateItemCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface ExpenseClaimCommandUseCase {

    ExpenseClaimId create(CreateClaimCommand command);

    ExpenseItemId addItem(AddItemCommand command);

    void updateItem(UpdateItemCommand command);

    void removeItem(RemoveItemCommand command);

    void submit(ClaimCommand command);

    void approveByManager(ApproveCommand command);

    void approveByFinance(ApproveCommand command);

    void reject(RejectClaimCommand command);

    void reopen(ClaimCommand command);

    void withdraw(ClaimCommand command);
}
