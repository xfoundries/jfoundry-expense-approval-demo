package io.github.xfoundries.demo.expenseapproval.infrastructure.query.claim;

import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.application.approval.ApprovedExpenseAmountReader;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseClaimMapper;
import org.jfoundry.infrastructure.persistence.PersistenceFailureTranslator;
import org.jfoundry.infrastructure.persistence.PersistenceOperation;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisApprovedExpenseAmountReader implements ApprovedExpenseAmountReader {

    private final ExpenseClaimMapper mapper;
    private final PersistenceFailureTranslator failureTranslator;

    public MybatisApprovedExpenseAmountReader(
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
