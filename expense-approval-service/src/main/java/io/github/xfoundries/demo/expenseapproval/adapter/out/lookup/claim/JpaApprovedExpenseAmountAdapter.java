package io.github.xfoundries.demo.expenseapproval.adapter.out.lookup.claim;

import java.math.BigDecimal;
import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.application.approval.port.out.ApprovedExpenseAmountPort;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import jakarta.persistence.EntityManager;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.infrastructure.persistence.AbstractPersistenceAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@SecondaryAdapter
@Component
@Transactional(readOnly = true)
public class JpaApprovedExpenseAmountAdapter
        extends AbstractPersistenceAdapter
        implements ApprovedExpenseAmountPort {

    private final EntityManager entityManager;

    public JpaApprovedExpenseAmountAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Money totalApproved(
            UserId employeeId, Instant fromInclusive, Instant toExclusive) {
        return query(() -> new Money(entityManager.createQuery("""
                        select coalesce(sum(c.totalAmount), 0)
                        from ExpenseClaimEntity c
                        where c.claimantId = :employeeId
                          and c.state = 'APPROVED'
                          and c.completedAt >= :fromInclusive
                          and c.completedAt < :toExclusive
                        """, BigDecimal.class)
                .setParameter("employeeId", employeeId.value())
                .setParameter("fromInclusive", fromInclusive)
                .setParameter("toExclusive", toExclusive)
                .getSingleResult()));
    }
}
