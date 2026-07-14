package io.github.xfoundries.demo.contracts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventEnvelopeTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-07-13T08:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void roundTripsApprovedEventWithVersionedMetadata() throws Exception {
        EventEnvelope<ExpenseClaimApprovedV1> envelope = EventEnvelope.create(
                "event-1",
                "ExpenseClaimApproved",
                1,
                OCCURRED_AT,
                "correlation-1",
                null,
                "claim-1",
                new ExpenseClaimApprovedV1(
                        "claim-1",
                        "employee-1",
                        new BigDecimal("100.00"),
                        "CNY",
                        OCCURRED_AT));

        String json = objectMapper.writeValueAsString(envelope);
        EventEnvelope<ExpenseClaimApprovedV1> restored = objectMapper.readValue(
                json, new TypeReference<>() {
                });

        assertThat(restored).isEqualTo(envelope);
        assertThat(json).contains("\"eventVersion\":1");
    }

    @Test
    void supportsPaidAndFailedPayloads() {
        ReimbursementPaidV1 paid = new ReimbursementPaidV1(
                "claim-1", "payment-1", OCCURRED_AT);
        ReimbursementPaymentFailedV1 failed = new ReimbursementPaymentFailedV1(
                "claim-2", "SINGLE_PAYMENT_LIMIT_EXCEEDED",
                "Single reimbursement exceeds the simulator limit", OCCURRED_AT);

        assertThat(paid.claimId()).isEqualTo("claim-1");
        assertThat(failed.failureCode()).isEqualTo("SINGLE_PAYMENT_LIMIT_EXCEEDED");
    }

    @Test
    void rejectsInvalidEnvelopeMetadata() {
        ExpenseClaimApprovedV1 payload = new ExpenseClaimApprovedV1(
                "claim-1", "employee-1", new BigDecimal("1.00"), "CNY", OCCURRED_AT);

        assertThatThrownBy(() -> EventEnvelope.create(
                " ", "ExpenseClaimApproved", 1, OCCURRED_AT,
                "correlation-1", null, "claim-1", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventId must not be blank");
        assertThatThrownBy(() -> EventEnvelope.create(
                "event-1", "ExpenseClaimApproved", 0, OCCURRED_AT,
                "correlation-1", null, "claim-1", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventVersion must be positive");
        assertThatThrownBy(() -> EventEnvelope.create(
                "event-1", "ExpenseClaimApproved", 1, OCCURRED_AT,
                "correlation-1", null, "claim-1", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("payload must not be null");
    }
}
