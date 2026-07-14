package io.github.xfoundries.demo.expenseapproval.application.command;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import org.jfoundry.architecture.cqrs.Command;

@Command
public record RemoveExpenseItemCommand(
        Actor actor, ExpenseClaimId claimId, ExpenseItemId itemId) {
}
