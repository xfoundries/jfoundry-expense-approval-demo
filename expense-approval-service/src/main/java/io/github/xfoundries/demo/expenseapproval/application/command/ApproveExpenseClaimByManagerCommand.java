package io.github.xfoundries.demo.expenseapproval.application.command;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import org.jfoundry.architecture.cqrs.Command;

@Command
public record ApproveExpenseClaimByManagerCommand(Actor actor, ExpenseClaimId claimId) {
}
