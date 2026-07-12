package io.github.xfoundries.demo.expenseapproval.domain.repository;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import org.jfoundry.domain.repository.AggregateRepository;

public interface ExpenseClaimRepository
        extends AggregateRepository<ExpenseClaim, ExpenseClaimId> {
}
