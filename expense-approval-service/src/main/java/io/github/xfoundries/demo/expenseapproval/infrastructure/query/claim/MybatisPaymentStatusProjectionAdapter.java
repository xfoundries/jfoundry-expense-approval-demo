package io.github.xfoundries.demo.expenseapproval.infrastructure.query.claim;

import io.github.xfoundries.demo.expenseapproval.application.port.out.PaymentStatusProjectionPort;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.infrastructure.persistence.PersistenceFailureTranslator;
import org.jfoundry.infrastructure.persistence.PersistenceOperation;
import org.springframework.stereotype.Repository;

@Repository
@SecondaryAdapter
public class MybatisPaymentStatusProjectionAdapter implements PaymentStatusProjectionPort {

    private final PaymentStatusMapper mapper;
    private final PersistenceFailureTranslator failureTranslator;

    public MybatisPaymentStatusProjectionAdapter(
            PaymentStatusMapper mapper,
            PersistenceFailureTranslator failureTranslator) {
        this.mapper = mapper;
        this.failureTranslator = failureTranslator;
    }

    @Override
    public void upsert(PaymentStatusProjection projection) {
        try {
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
        } catch (RuntimeException failure) {
            throw failureTranslator.translate(PersistenceOperation.MODIFY, failure);
        }
    }
}
