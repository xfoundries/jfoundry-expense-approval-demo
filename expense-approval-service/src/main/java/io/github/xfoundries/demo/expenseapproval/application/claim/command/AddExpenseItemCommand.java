package io.github.xfoundries.demo.expenseapproval.application.claim.command;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseCategory;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import org.jfoundry.architecture.cqrs.Command;

@Command
public record AddExpenseItemCommand(
        ApprovalActor actor,
        ExpenseClaimId claimId,
        LocalDate expenseDate,
        ExpenseCategory category,
        BigDecimal amount,
        String description,
        String receiptReference) {
}
