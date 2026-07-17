package io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim;

import java.sql.SQLException;
import java.util.Collection;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import org.jfoundry.application.exception.ConflictException;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

@SecondaryAdapter
@Component
public class JpaExpenseClaimRepositoryAdapter implements ExpenseClaimRepository {

    private final JpaExpenseClaimRepository repository;

    public JpaExpenseClaimRepositoryAdapter(JpaExpenseClaimRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExpenseClaim findById(ExpenseClaimId id) {
        return repository.findById(id);
    }

    @Override
    public void add(ExpenseClaim claim) {
        try {
            repository.add(claim);
        } catch (RuntimeException failure) {
            throw translateDuplicateInsert(claim, failure);
        }
    }

    @Override
    public void modify(ExpenseClaim claim) {
        try {
            repository.modify(claim);
        } catch (RuntimeException failure) {
            throw translateActionHistoryConflict(claim, failure);
        }
    }

    @Override
    public void addAll(Collection<ExpenseClaim> claims) {
        claims.forEach(this::add);
    }

    @Override
    public void modifyAll(Collection<ExpenseClaim> claims) {
        repository.modifyAll(claims);
    }

    @Override
    public void remove(ExpenseClaim claim) {
        repository.remove(claim);
    }

    private static RuntimeException translateDuplicateInsert(
            ExpenseClaim claim, RuntimeException failure) {
        SQLException sqlException = findSqlException(failure);
        if (sqlException != null && "23505".equals(sqlException.getSQLState())) {
            return new ConflictException(
                    "Expense claim already exists: " + claim.id().value(), failure);
        }
        return failure;
    }

    private static SQLException findSqlException(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    private static RuntimeException translateActionHistoryConflict(
            ExpenseClaim claim, RuntimeException failure) {
        SQLException sqlException = findSqlException(failure);
        if (sqlException != null
                && "23505".equals(sqlException.getSQLState())
                && isActionHistoryConstraint(sqlException)) {
            return new ConflictException(
                    "modify optimistic lock conflict for aggregate: " + claim.id().value(), failure);
        }
        return failure;
    }

    private static boolean isActionHistoryConstraint(SQLException failure) {
        String message = failure.getMessage();
        return message != null
                && (message.contains("claim_action_pkey")
                || message.contains("uq_claim_action_sequence"));
    }
}
