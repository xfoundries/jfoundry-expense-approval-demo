package io.github.xfoundries.demo.expenseapproval.infrastructure.query.claim;

import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.application.port.out.MonthlyApprovedAmountPort;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseClaimMapper;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.infrastructure.persistence.PersistenceFailureTranslator;
import org.jfoundry.infrastructure.persistence.PersistenceOperation;
import org.springframework.stereotype.Repository;

@Repository
@SecondaryAdapter
public class MybatisMonthlyApprovedAmountAdapter implements MonthlyApprovedAmountPort {

    private final ExpenseClaimMapper mapper;
    private final PersistenceFailureTranslator failureTranslator;

    public MybatisMonthlyApprovedAmountAdapter(
            ExpenseClaimMapper mapper, PersistenceFailureTranslator failureTranslator) {
        this.mapper = mapper;
        this.failureTranslator = failureTranslator;
    }

    @Override
    public Money totalApproved(
            UserId employeeId, Instant fromInclusive, Instant toExclusive) {
        try {
            return new Money(mapper.sumApprovedAmount(
                    employeeId.value(), fromInclusive, toExclusive));
        } catch (RuntimeException failure) {
            throw failureTranslator.translate(PersistenceOperation.QUERY, failure);
        }
    }
}
