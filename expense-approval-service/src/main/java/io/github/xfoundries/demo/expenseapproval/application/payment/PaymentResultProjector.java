package io.github.xfoundries.demo.expenseapproval.application.payment;

import java.util.Objects;

import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ReimbursementPaidV1;
import io.github.xfoundries.demo.contracts.ReimbursementPaymentFailedV1;
import io.github.xfoundries.demo.expenseapproval.application.payment.port.out.PaymentStatusProjectionPort;
import io.github.xfoundries.demo.expenseapproval.application.payment.port.out.PaymentStatusProjectionPort.PaymentStatusProjection;
import org.jfoundry.application.inbox.InboxTemplate;

public class PaymentResultProjector {

    public static final String CONSUMER_NAME = "expense-approval.payment-result-projector.v1";

    private final InboxTemplate inboxTemplate;
    private final PaymentStatusProjectionPort projectionPort;

    public PaymentResultProjector(
            InboxTemplate inboxTemplate,
            PaymentStatusProjectionPort projectionPort) {
        this.inboxTemplate = Objects.requireNonNull(inboxTemplate);
        this.projectionPort = Objects.requireNonNull(projectionPort);
    }

    public boolean projectPaid(EventEnvelope<ReimbursementPaidV1> event) throws Exception {
        ReimbursementPaidV1 payload = event.payload();
        return project(event.eventId(), new PaymentStatusProjection(
                payload.claimId(), "PAID", payload.paymentReference(), null, null,
                payload.paidAt(), event.eventId()));
    }

    public boolean projectFailed(EventEnvelope<ReimbursementPaymentFailedV1> event) throws Exception {
        ReimbursementPaymentFailedV1 payload = event.payload();
        return project(event.eventId(), new PaymentStatusProjection(
                payload.claimId(), "FAILED", null, payload.failureCode(), payload.failureReason(),
                payload.failedAt(), event.eventId()));
    }

    private boolean project(String eventId, PaymentStatusProjection projection) throws Exception {
        return inboxTemplate.executeOnce(
                eventId, CONSUMER_NAME, () -> projectionPort.upsert(projection));
    }
}
