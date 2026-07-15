package io.github.xfoundries.demo.expenseapproval.adapter.out.lookup.claim;

import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim.ExpenseClaimMapper;
import io.github.xfoundries.demo.expenseapproval.application.approval.port.out.ApprovedExpenseAmountPort;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.infrastructure.persistence.AbstractPersistenceAdapter;
import org.springframework.stereotype.Repository;

@SecondaryAdapter
@Repository
public class MybatisApprovedExpenseAmountAdapter
        extends AbstractPersistenceAdapter
        implements ApprovedExpenseAmountPort {

    private final ExpenseClaimMapper mapper;

    public MybatisApprovedExpenseAmountAdapter(ExpenseClaimMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Money totalApproved(
            UserId employeeId, Instant fromInclusive, Instant toExclusive) {
        return query(() -> new Money(mapper.sumApprovedAmount(
                employeeId.value(), fromInclusive, toExclusive)));
    }
}
