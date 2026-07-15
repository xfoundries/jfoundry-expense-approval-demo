package io.github.xfoundries.demo.expenseapproval.infrastructure.lookup.claim;

import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.application.approval.ApprovedExpenseAmountReader;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseClaimMapper;
import org.jfoundry.infrastructure.persistence.AbstractPersistenceAdapter;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisApprovedExpenseAmountReader
        extends AbstractPersistenceAdapter
        implements ApprovedExpenseAmountReader {

    private final ExpenseClaimMapper mapper;

    public MybatisApprovedExpenseAmountReader(ExpenseClaimMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Money totalApproved(
            UserId employeeId, Instant fromInclusive, Instant toExclusive) {
        return query(() -> new Money(mapper.sumApprovedAmount(
                employeeId.value(), fromInclusive, toExclusive)));
    }
}
