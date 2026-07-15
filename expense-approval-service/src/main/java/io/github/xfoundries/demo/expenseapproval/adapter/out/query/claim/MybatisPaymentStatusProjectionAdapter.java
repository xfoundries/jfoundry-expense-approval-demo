package io.github.xfoundries.demo.expenseapproval.adapter.out.query.claim;

import io.github.xfoundries.demo.expenseapproval.application.payment.port.out.PaymentStatusProjectionPort;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.infrastructure.persistence.AbstractPersistenceAdapter;
import org.springframework.stereotype.Repository;

@SecondaryAdapter
@Repository
public class MybatisPaymentStatusProjectionAdapter
        extends AbstractPersistenceAdapter
        implements PaymentStatusProjectionPort {

    private final PaymentStatusMapper mapper;

    public MybatisPaymentStatusProjectionAdapter(PaymentStatusMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void upsert(PaymentStatusProjection projection) {
        modify(() -> {
            PaymentStatusData data = new PaymentStatusData();
            data.setClaimId(projection.claimId());
            data.setStatus(projection.status());
            data.setPaymentReference(projection.paymentReference());
            data.setFailureCode(projection.failureCode());
            data.setFailureReason(projection.failureReason());
            data.setProcessedAt(projection.processedAt());
            data.setSourceEventId(projection.sourceEventId());
            data.setUpdatedAt(projection.processedAt());
            if (!mapper.insertOrUpdate(data)) {
                throw new IllegalStateException("Payment status projection was not persisted");
            }
        });
    }
}
