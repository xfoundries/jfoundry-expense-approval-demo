package io.github.xfoundries.demo.payment.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ExpenseClaimApprovedV1;
import io.github.xfoundries.demo.payment.boot.PaymentProcessorSimulatorApplication;
import org.jfoundry.application.inbox.InboxTemplate;
import org.jfoundry.application.outbox.BackoffStrategy;
import org.jfoundry.application.outbox.OutboxMessage;
import org.jfoundry.application.outbox.OutboxMessageStatus;
import org.jfoundry.application.outbox.OutboxMessageStore;
import org.jfoundry.application.outbox.OutboxTemplate;
import org.jfoundry.application.transaction.TransactionRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(classes = PaymentProcessorSimulatorApplication.class)
class ExpenseClaimApprovedProcessorIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-13T08:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> DATABASE =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired ExpenseClaimApprovedProcessor processor;
    @Autowired InboxTemplate inboxTemplate;
    @Autowired TransactionRunner transactions;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void duplicateApprovalProducesOneInboxAndOneResultOutboxMessage() throws Exception {
        EventEnvelope<ExpenseClaimApprovedV1> approval = approval("approval-once", "claim-once");

        assertThat(processor.process(approval)).isTrue();
        assertThat(processor.process(approval)).isFalse();

        assertThat(count("jfoundry_inbox_message", "message_id", "approval-once")).isEqualTo(1);
        assertThat(count("jfoundry_outbox_event", "payload_key", "claim-once")).isEqualTo(1);
    }

    @Test
    void outboxFailureIsRecordedAndAllowsRetry() throws Exception {
        EventEnvelope<ExpenseClaimApprovedV1> approval = approval("approval-retry", "claim-retry");
        ExpenseClaimApprovedProcessor failingProcessor = new ExpenseClaimApprovedProcessor(
                inboxTemplate,
                transactions,
                new OutboxTemplate(new FailingOutboxStore(), payload -> "{}"),
                new PaymentRule(new BigDecimal("8000.00"), "CNY"),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "failed-result-event");

        assertThatThrownBy(() -> failingProcessor.process(approval))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox unavailable");
        assertThat(count("jfoundry_inbox_message", "message_id", "approval-retry")).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select status from jfoundry_inbox_message where message_id = ?",
                String.class, "approval-retry")).isEqualTo("FAILED");
        assertThat(count("jfoundry_outbox_event", "payload_key", "claim-retry")).isZero();

        assertThat(processor.process(approval)).isTrue();
        assertThat(count("jfoundry_inbox_message", "message_id", "approval-retry")).isEqualTo(1);
        assertThat(count("jfoundry_outbox_event", "payload_key", "claim-retry")).isEqualTo(1);
    }

    private static EventEnvelope<ExpenseClaimApprovedV1> approval(String eventId, String claimId) {
        return EventEnvelope.create(
                eventId,
                "ExpenseClaimApproved",
                1,
                NOW.minusSeconds(60),
                "correlation-1",
                null,
                claimId,
                new ExpenseClaimApprovedV1(
                        claimId, "employee-1", new BigDecimal("100.00"), "CNY", NOW.minusSeconds(60)));
    }

    private int count(String table, String column, String value) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + column + " = ?",
                Integer.class,
                value);
    }

    private static final class FailingOutboxStore implements OutboxMessageStore {

        @Override
        public void append(OutboxMessage entry) {
            throw new IllegalStateException("outbox unavailable");
        }

        @Override
        public List<OutboxMessage> findDispatchable(int limit, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markAsPublished(String eventId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markAsFailed(
                String eventId, String errorMessage, int maxRetries, BackoffStrategy backoff) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reactivate(String eventId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OutboxMessage> claimDispatchable(int limit, String claimerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int recoverStuckDispatching(Instant cutoff) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteByStatusAndOccurredAtBefore(
                OutboxMessageStatus status, Instant cutoff, int batchSize) {
            throw new UnsupportedOperationException();
        }
    }
}
