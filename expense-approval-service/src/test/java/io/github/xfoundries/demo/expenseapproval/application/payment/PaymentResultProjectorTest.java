package io.github.xfoundries.demo.expenseapproval.application.payment;

import java.time.Instant;

import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ReimbursementPaidV1;
import io.github.xfoundries.demo.contracts.ReimbursementPaymentFailedV1;
import io.github.xfoundries.demo.expenseapproval.application.payment.port.out.PaymentStatusProjectionPort;
import io.github.xfoundries.demo.expenseapproval.application.payment.port.out.PaymentStatusProjectionPort.PaymentStatusProjection;
import org.jfoundry.application.inbox.InboxMessageStore;
import org.jfoundry.application.inbox.InboxTemplate;
import org.jfoundry.application.transaction.TransactionCallback;
import org.jfoundry.application.transaction.TransactionOptions;
import org.jfoundry.application.transaction.TransactionRunner;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentResultProjectorTest {

    private static final Instant NOW = Instant.parse("2026-07-13T08:00:00Z");

    private final InboxMessageStore inboxStore = mock(InboxMessageStore.class);
    private final PaymentStatusProjectionPort projectionPort = mock(PaymentStatusProjectionPort.class);
    private final TransactionRunner transactions = new TransactionRunner() {
        @Override
        public <T> T call(TransactionOptions options, TransactionCallback<T> callback) throws Exception {
            return callback.execute();
        }
    };
    private final PaymentResultProjector projector = new PaymentResultProjector(
            new InboxTemplate(inboxStore, transactions), projectionPort);

    @Test
    void projectsPaidEventOnlyOnce() throws Exception {
        when(inboxStore.tryStartProcessing(
                "paid-event", PaymentResultProjector.CONSUMER_NAME)).thenReturn(true, false);
        EventEnvelope<ReimbursementPaidV1> event = EventEnvelope.create(
                "paid-event", "ReimbursementPaid", 1, NOW, "claim-1", "approval-event",
                "claim-1", new ReimbursementPaidV1("claim-1", "payment-1", NOW));

        assertThat(projector.projectPaid(event)).isTrue();
        assertThat(projector.projectPaid(event)).isFalse();

        ArgumentCaptor<PaymentStatusProjection> projection =
                ArgumentCaptor.forClass(PaymentStatusProjection.class);
        verify(projectionPort).upsert(projection.capture());
        assertThat(projection.getValue().status()).isEqualTo("PAID");
        assertThat(projection.getValue().paymentReference()).isEqualTo("payment-1");
    }

    @Test
    void mapsPaymentFailureDetails() throws Exception {
        when(inboxStore.tryStartProcessing(
                "failed-event", PaymentResultProjector.CONSUMER_NAME)).thenReturn(true);
        EventEnvelope<ReimbursementPaymentFailedV1> event = EventEnvelope.create(
                "failed-event", "ReimbursementPaymentFailed", 1, NOW, "claim-1", "approval-event",
                "claim-1", new ReimbursementPaymentFailedV1(
                        "claim-1", "LIMIT", "Payment limit exceeded", NOW));

        assertThat(projector.projectFailed(event)).isTrue();

        ArgumentCaptor<PaymentStatusProjection> projection =
                ArgumentCaptor.forClass(PaymentStatusProjection.class);
        verify(projectionPort).upsert(projection.capture());
        assertThat(projection.getValue().status()).isEqualTo("FAILED");
        assertThat(projection.getValue().failureCode()).isEqualTo("LIMIT");
    }
}
