package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByFinanceCommand;
import io.github.xfoundries.demo.expenseapproval.application.approval.FinalApprovalCoordinator;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class ApproveExpenseClaimByFinanceCommandHandler {

    private final FinalApprovalCoordinator coordinator;

    public ApproveExpenseClaimByFinanceCommandHandler(FinalApprovalCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @CommandHandler
    public void handle(ApproveExpenseClaimByFinanceCommand command) {
        coordinator.approveByFinance(command);
    }
}
