package io.github.xfoundries.demo.expenseapproval.application.approval;

import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;

public interface ApprovedExpenseAmountReader {

    Money totalApproved(UserId employeeId, Instant fromInclusive, Instant toExclusive);
}
