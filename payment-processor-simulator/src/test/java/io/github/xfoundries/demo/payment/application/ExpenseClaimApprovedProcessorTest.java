package io.github.xfoundries.demo.payment.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ExpenseClaimApprovedV1;
import io.github.xfoundries.demo.contracts.ReimbursementPaidV1;
import io.github.xfoundries.demo.contracts.ReimbursementPaymentFailedV1;
import org.jfoundry.application.inbox.InboxMessageStore;
import org.jfoundry.application.inbox.InboxTemplate;
import org.jfoundry.application.outbox.OutboxAppendRequest;
import org.jfoundry.application.outbox.OutboxTemplate;
import org.jfoundry.application.transaction.TransactionCallback;
import org.jfoundry.application.transaction.TransactionOptions;
import org.jfoundry.application.transaction.TransactionRunner;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseClaimApprovedProcessorTest {

    private static final Instant NOW = Instant.parse("2026-07-13T08:00:00Z");

    private final InboxMessageStore inboxStore = mock(InboxMessageStore.class);
    private final OutboxTemplate outboxTemplate = mock(OutboxTemplate.class);
    private final TransactionRunner transactions = new TransactionRunner() {
        @Override
        public <T> T call(TransactionOptions options, TransactionCallback<T> callback) throws Exception {
            return callback.execute();
        }
    };
    private final ExpenseClaimApprovedProcessor processor = new ExpenseClaimApprovedProcessor(
            new InboxTemplate(inboxStore, transactions),
            outboxTemplate,
            new PaymentRule(new BigDecimal("8000.00"), "CNY"),
            Clock.fixed(NOW, ZoneOffset.UTC),
            () -> "payment-event-1");

    @Test
    void recordsOnePaidResultForDuplicateApprovalDelivery() throws Exception {
        when(inboxStore.tryStartProcessing(
                "approval-event", ExpenseClaimApprovedProcessor.CONSUMER_NAME))
                .thenReturn(true, false);
        EventEnvelope<ExpenseClaimApprovedV1> approval = approval(new BigDecimal("8000.00"));

        assertThat(processor.process(approval)).isTrue();
        assertThat(processor.process(approval)).isFalse();

        ArgumentCaptor<OutboxAppendRequest> request = ArgumentCaptor.forClass(OutboxAppendRequest.class);
        verify(outboxTemplate).append(request.capture());
        assertThat(request.getValue().topic()).isEqualTo("payment.events.v1");
        assertThat(request.getValue().payloadKey()).isEqualTo("claim-1");
        assertThat(request.getValue().payloadType()).isEqualTo("ReimbursementPaidV1");
        assertThat(request.getValue().eventId()).isEqualTo("payment-event-1");

        @SuppressWarnings("unchecked")
        EventEnvelope<ReimbursementPaidV1> result =
                (EventEnvelope<ReimbursementPaidV1>) request.getValue().payload();
        assertThat(result.correlationId()).isEqualTo("correlation-1");
        assertThat(result.causationId()).isEqualTo("approval-event");
        assertThat(result.payload().paymentReference()).isEqualTo("PAY-claim-1");
        assertThat(result.payload().paidAt()).isEqualTo(NOW);
    }

    @Test
    void recordsFailedResultAboveThePaymentLimit() throws Exception {
        when(inboxStore.tryStartProcessing(
                "approval-event", ExpenseClaimApprovedProcessor.CONSUMER_NAME)).thenReturn(true);

        assertThat(processor.process(approval(new BigDecimal("8000.01")))).isTrue();

        ArgumentCaptor<OutboxAppendRequest> request = ArgumentCaptor.forClass(OutboxAppendRequest.class);
        verify(outboxTemplate).append(request.capture());
        assertThat(request.getValue().payloadType()).isEqualTo("ReimbursementPaymentFailedV1");

        @SuppressWarnings("unchecked")
        EventEnvelope<ReimbursementPaymentFailedV1> result =
                (EventEnvelope<ReimbursementPaymentFailedV1>) request.getValue().payload();
        assertThat(result.payload().failureCode()).isEqualTo("SINGLE_PAYMENT_LIMIT_EXCEEDED");
        assertThat(result.payload().failureReason())
                .isEqualTo("Approved amount exceeds the single-payment limit of 8000.00 CNY");
        assertThat(result.payload().failedAt()).isEqualTo(NOW);
    }

    private static EventEnvelope<ExpenseClaimApprovedV1> approval(BigDecimal amount) {
        return EventEnvelope.create(
                "approval-event",
                "ExpenseClaimApproved",
                1,
                NOW.minusSeconds(60),
                "correlation-1",
                null,
                "claim-1",
                new ExpenseClaimApprovedV1("claim-1", "employee-1", amount, "CNY", NOW.minusSeconds(60)));
    }
}
