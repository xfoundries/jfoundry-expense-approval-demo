package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in.ApproveExpenseClaimByManagerUseCase;
import io.github.xfoundries.demo.expenseapproval.application.approval.FinalApprovalCoordinator;
import org.jfoundry.architecture.cqrs.CommandHandler;

public class ApproveExpenseClaimByManagerCommandHandler implements ApproveExpenseClaimByManagerUseCase {

    private final FinalApprovalCoordinator coordinator;

    public ApproveExpenseClaimByManagerCommandHandler(FinalApprovalCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    @CommandHandler
    public void approveByManager(ApproveExpenseClaimByManagerCommand command) {
        coordinator.approveByManager(command);
    }
}
