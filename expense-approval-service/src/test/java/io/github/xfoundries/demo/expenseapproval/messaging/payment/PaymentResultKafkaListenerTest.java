package io.github.xfoundries.demo.expenseapproval.messaging.payment;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ReimbursementPaidV1;
import io.github.xfoundries.demo.contracts.ReimbursementPaymentFailedV1;
import io.github.xfoundries.demo.expenseapproval.application.payment.PaymentResultProjector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentResultKafkaListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-13T08:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final PaymentResultProjector projector = mock(PaymentResultProjector.class);
    private final PaymentResultKafkaListener listener =
            new PaymentResultKafkaListener(objectMapper, projector);

    @Test
    void routesPaidEvent() throws Exception {
        EventEnvelope<ReimbursementPaidV1> event = EventEnvelope.create(
                "paid-event", "ReimbursementPaid", 1, NOW, "correlation-1", "approval-event",
                "claim-1", new ReimbursementPaidV1("claim-1", "PAY-claim-1", NOW));

        listener.onMessage(objectMapper.writeValueAsString(event));

        verify(projector).projectPaid(event);
    }

    @Test
    void routesFailedEvent() throws Exception {
        EventEnvelope<ReimbursementPaymentFailedV1> event = EventEnvelope.create(
                "failed-event", "ReimbursementPaymentFailed", 1, NOW,
                "correlation-1", "approval-event", "claim-1",
                new ReimbursementPaymentFailedV1("claim-1", "LIMIT", "Payment limit exceeded", NOW));

        listener.onMessage(objectMapper.writeValueAsString(event));

        verify(projector).projectFailed(event);
    }

    @Test
    void rejectsUnknownPaymentEventType() throws Exception {
        EventEnvelope<ReimbursementPaidV1> event = EventEnvelope.create(
                "unknown-event", "UnknownPaymentResult", 1, NOW,
                "correlation-1", "approval-event", "claim-1",
                new ReimbursementPaidV1("claim-1", "PAY-claim-1", NOW));

        assertThatThrownBy(() -> listener.onMessage(objectMapper.writeValueAsString(event)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported payment event type: UnknownPaymentResult");
    }
}
