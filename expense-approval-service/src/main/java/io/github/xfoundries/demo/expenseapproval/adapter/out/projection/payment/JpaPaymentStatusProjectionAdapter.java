package io.github.xfoundries.demo.expenseapproval.adapter.out.projection.payment;

import io.github.xfoundries.demo.expenseapproval.application.payment.port.out.PaymentStatusProjectionPort;
import jakarta.persistence.EntityManager;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.infrastructure.persistence.AbstractPersistenceAdapter;
import org.springframework.stereotype.Component;

@SecondaryAdapter
@Component
public class JpaPaymentStatusProjectionAdapter
        extends AbstractPersistenceAdapter
        implements PaymentStatusProjectionPort {

    private final EntityManager entityManager;

    public JpaPaymentStatusProjectionAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void upsert(PaymentStatusProjection projection) {
        modify(() -> {
            PaymentStatusEntity existing = entityManager.find(
                    PaymentStatusEntity.class, projection.claimId());
            if (existing == null) {
                entityManager.persist(new PaymentStatusEntity(
                        projection.claimId(),
                        projection.status(),
                        projection.paymentReference(),
                        projection.failureCode(),
                        projection.failureReason(),
                        projection.processedAt(),
                        projection.sourceEventId()));
                return;
            }
            existing.apply(
                    projection.status(),
                    projection.paymentReference(),
                    projection.failureCode(),
                    projection.failureReason(),
                    projection.processedAt(),
                    projection.sourceEventId());
        });
    }
}
