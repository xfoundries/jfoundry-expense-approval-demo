package io.github.xfoundries.demo.expenseapproval.domain.repository;

import java.util.Optional;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import org.jmolecules.ddd.types.Repository;

public interface ExpenseClaimRepository extends Repository<ExpenseClaim, ExpenseClaimId> {

    Optional<ExpenseClaim> findById(ExpenseClaimId id);

    void save(ExpenseClaim claim);
}

