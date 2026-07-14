package io.github.xfoundries.demo.payment.messaging.approval;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ExpenseClaimApprovedV1;
import io.github.xfoundries.demo.payment.application.ExpenseClaimApprovedProcessor;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExpenseClaimApprovedKafkaListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-13T08:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ExpenseClaimApprovedProcessor processor = mock(ExpenseClaimApprovedProcessor.class);
    private final ExpenseClaimApprovedKafkaListener listener =
            new ExpenseClaimApprovedKafkaListener(objectMapper, processor);

    @Test
    void deserializesAndProcessesApprovedClaim() throws Exception {
        EventEnvelope<ExpenseClaimApprovedV1> event = EventEnvelope.create(
                "approval-event", "ExpenseClaimApproved", 1, NOW,
                "correlation-1", null, "claim-1",
                new ExpenseClaimApprovedV1(
                        "claim-1", "employee-1", new BigDecimal("100.00"), "CNY", NOW));

        listener.onMessage(objectMapper.writeValueAsString(event));

        verify(processor).process(event);
    }
}
