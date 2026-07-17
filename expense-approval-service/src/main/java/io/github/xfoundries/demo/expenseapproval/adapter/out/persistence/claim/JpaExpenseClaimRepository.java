package io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import jakarta.persistence.EntityManager;
import org.jfoundry.infrastructure.persistence.jpa.JpaAggregateRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaExpenseClaimRepository
        extends JpaAggregateRepository<ExpenseClaim, ExpenseClaimId, ExpenseClaimEntity, String> {

    public JpaExpenseClaimRepository(EntityManager entityManager) {
        super(entityManager, ExpenseClaimEntity.class, new ExpenseClaimJpaMapper());
    }
}
