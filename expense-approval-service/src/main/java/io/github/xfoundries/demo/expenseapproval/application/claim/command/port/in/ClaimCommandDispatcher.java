package io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.AddExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByFinanceCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.CreateExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RejectExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RemoveExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ReopenExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.SubmitExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.UpdateExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.WithdrawExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import org.jfoundry.architecture.cqrs.CommandDispatcher;
import org.jfoundry.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface ClaimCommandDispatcher {

    @CommandDispatcher
    ExpenseClaimId dispatch(CreateExpenseClaimCommand command);

    @CommandDispatcher
    ExpenseItemId dispatch(AddExpenseItemCommand command);

    @CommandDispatcher
    void dispatch(UpdateExpenseItemCommand command);

    @CommandDispatcher
    void dispatch(RemoveExpenseItemCommand command);

    @CommandDispatcher
    void dispatch(SubmitExpenseClaimCommand command);

    @CommandDispatcher
    void dispatch(ApproveExpenseClaimByManagerCommand command);

    @CommandDispatcher
    void dispatch(ApproveExpenseClaimByFinanceCommand command);

    @CommandDispatcher
    void dispatch(RejectExpenseClaimCommand command);

    @CommandDispatcher
    void dispatch(ReopenExpenseClaimCommand command);

    @CommandDispatcher
    void dispatch(WithdrawExpenseClaimCommand command);
}
