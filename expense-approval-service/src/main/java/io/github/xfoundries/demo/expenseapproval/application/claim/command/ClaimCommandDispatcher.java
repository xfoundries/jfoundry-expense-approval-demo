package io.github.xfoundries.demo.expenseapproval.application.claim.command;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import org.jfoundry.architecture.cqrs.CommandDispatcher;

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
