package io.github.xfoundries.demo.expenseapproval.application.port.out;

import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import org.jfoundry.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface MonthlyApprovedAmountPort {

    Money totalApproved(UserId employeeId, Instant fromInclusive, Instant toExclusive);
}
