package io.github.xfoundries.demo.expenseapproval.application.approval.port.out;

import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import org.jfoundry.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface ApprovedExpenseAmountPort {

    Money totalApproved(UserId employeeId, Instant fromInclusive, Instant toExclusive);
}
