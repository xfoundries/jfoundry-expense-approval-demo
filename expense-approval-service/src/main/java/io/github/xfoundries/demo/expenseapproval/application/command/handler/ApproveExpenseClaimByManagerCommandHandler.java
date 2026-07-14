package io.github.xfoundries.demo.expenseapproval.application.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.FinalApprovalCoordinator;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class ApproveExpenseClaimByManagerCommandHandler {

    private final FinalApprovalCoordinator coordinator;

    public ApproveExpenseClaimByManagerCommandHandler(FinalApprovalCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @CommandHandler
    public void handle(ApproveExpenseClaimByManagerCommand command) {
        coordinator.approveByManager(command);
    }
}
