package io.github.xfoundries.demo.expenseapproval.application.claim.command;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in.ClaimCommandDispatcher;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.AddExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ApproveExpenseClaimByFinanceCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ApproveExpenseClaimByManagerCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.CreateExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.RejectExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.RemoveExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ReopenExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.SubmitExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.UpdateExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.WithdrawExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;

public final class DefaultClaimCommandDispatcher implements ClaimCommandDispatcher {

    private final CreateExpenseClaimCommandHandler createHandler;
    private final AddExpenseItemCommandHandler addItemHandler;
    private final UpdateExpenseItemCommandHandler updateItemHandler;
    private final RemoveExpenseItemCommandHandler removeItemHandler;
    private final SubmitExpenseClaimCommandHandler submitHandler;
    private final ApproveExpenseClaimByManagerCommandHandler managerApprovalHandler;
    private final ApproveExpenseClaimByFinanceCommandHandler financeApprovalHandler;
    private final RejectExpenseClaimCommandHandler rejectHandler;
    private final ReopenExpenseClaimCommandHandler reopenHandler;
    private final WithdrawExpenseClaimCommandHandler withdrawHandler;

    public DefaultClaimCommandDispatcher(
            CreateExpenseClaimCommandHandler createHandler,
            AddExpenseItemCommandHandler addItemHandler,
            UpdateExpenseItemCommandHandler updateItemHandler,
            RemoveExpenseItemCommandHandler removeItemHandler,
            SubmitExpenseClaimCommandHandler submitHandler,
            ApproveExpenseClaimByManagerCommandHandler managerApprovalHandler,
            ApproveExpenseClaimByFinanceCommandHandler financeApprovalHandler,
            RejectExpenseClaimCommandHandler rejectHandler,
            ReopenExpenseClaimCommandHandler reopenHandler,
            WithdrawExpenseClaimCommandHandler withdrawHandler) {
        this.createHandler = createHandler;
        this.addItemHandler = addItemHandler;
        this.updateItemHandler = updateItemHandler;
        this.removeItemHandler = removeItemHandler;
        this.submitHandler = submitHandler;
        this.managerApprovalHandler = managerApprovalHandler;
        this.financeApprovalHandler = financeApprovalHandler;
        this.rejectHandler = rejectHandler;
        this.reopenHandler = reopenHandler;
        this.withdrawHandler = withdrawHandler;
    }

    @Override
    public ExpenseClaimId dispatch(CreateExpenseClaimCommand command) {
        return createHandler.handle(command);
    }

    @Override
    public ExpenseItemId dispatch(AddExpenseItemCommand command) {
        return addItemHandler.handle(command);
    }

    @Override
    public void dispatch(UpdateExpenseItemCommand command) {
        updateItemHandler.handle(command);
    }

    @Override
    public void dispatch(RemoveExpenseItemCommand command) {
        removeItemHandler.handle(command);
    }

    @Override
    public void dispatch(SubmitExpenseClaimCommand command) {
        submitHandler.handle(command);
    }

    @Override
    public void dispatch(ApproveExpenseClaimByManagerCommand command) {
        managerApprovalHandler.handle(command);
    }

    @Override
    public void dispatch(ApproveExpenseClaimByFinanceCommand command) {
        financeApprovalHandler.handle(command);
    }

    @Override
    public void dispatch(RejectExpenseClaimCommand command) {
        rejectHandler.handle(command);
    }

    @Override
    public void dispatch(ReopenExpenseClaimCommand command) {
        reopenHandler.handle(command);
    }

    @Override
    public void dispatch(WithdrawExpenseClaimCommand command) {
        withdrawHandler.handle(command);
    }
}
