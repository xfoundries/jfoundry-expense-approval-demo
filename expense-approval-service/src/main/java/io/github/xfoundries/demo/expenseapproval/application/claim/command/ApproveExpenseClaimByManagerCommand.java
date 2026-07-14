package io.github.xfoundries.demo.expenseapproval.application.claim.command;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import org.jfoundry.architecture.cqrs.Command;

@Command
public record ApproveExpenseClaimByManagerCommand(ApprovalActor actor, ExpenseClaimId claimId) {
}
