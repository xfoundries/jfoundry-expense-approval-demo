package io.github.xfoundries.demo.expenseapproval.application.payment;

import java.time.Instant;
import java.time.ZoneOffset;

import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ReimbursementPaidV1;
import io.github.xfoundries.demo.expenseapproval.application.payment.port.out.PaymentStatusProjectionPort;
import io.github.xfoundries.demo.expenseapproval.boot.ExpenseApprovalApplication;
import io.github.xfoundries.demo.expenseapproval.support.PostgreSqlIntegrationTest;
import org.jfoundry.application.inbox.InboxTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = ExpenseApprovalApplication.class)
class PaymentResultProjectorIntegrationTest extends PostgreSqlIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-13T08:00:00Z");

    @Autowired PaymentResultProjector projector;
    @Autowired InboxTemplate inboxTemplate;
    @Autowired PaymentStatusProjectionPort projectionPort;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void duplicateEventProducesOneProcessedInboxAndOneProjection() throws Exception {
        insertApprovedClaim("claim-paid");
        EventEnvelope<ReimbursementPaidV1> event = paidEvent("paid-event", "claim-paid");

        assertThat(projector.projectPaid(event)).isTrue();
        assertThat(projector.projectPaid(event)).isFalse();

        assertThat(count("claim_payment_status", "claim_id", "claim-paid")).isEqualTo(1);
        assertThat(count("jfoundry_inbox_message", "message_id", "paid-event")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select status from jfoundry_inbox_message where message_id = ?",
                String.class, "paid-event")).isEqualTo("PROCESSED");
    }

    @Test
    void failedProjectionIsRecordedAndCanBeRetried() throws Exception {
        insertApprovedClaim("claim-retry");
        EventEnvelope<ReimbursementPaidV1> event = paidEvent("retry-event", "claim-retry");
        PaymentResultProjector failingProjector = new PaymentResultProjector(
                inboxTemplate, projection -> {
                    throw new IllegalStateException("fail once");
                });

        assertThatThrownBy(() -> failingProjector.projectPaid(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fail once");
        assertThat(count("jfoundry_inbox_message", "message_id", "retry-event")).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select status from jfoundry_inbox_message where message_id = ?",
                String.class, "retry-event")).isEqualTo("FAILED");

        assertThat(projector.projectPaid(event)).isTrue();
        assertThat(count("claim_payment_status", "claim_id", "claim-retry")).isEqualTo(1);
    }

    private EventEnvelope<ReimbursementPaidV1> paidEvent(String eventId, String claimId) {
        return EventEnvelope.create(
                eventId, "ReimbursementPaid", 1, NOW, claimId, "approval-event",
                claimId, new ReimbursementPaidV1(claimId, "payment-" + claimId, NOW));
    }

    private void insertApprovedClaim(String claimId) {
        var timestamp = NOW.atOffset(ZoneOffset.UTC);
        jdbcTemplate.update("""
                insert into expense_claim(
                    id, claimant_id, title, state, total_amount, finance_approval_required,
                    created_at, updated_at, submitted_at, completed_at, version)
                values (?, 'employee-1', 'Approved', 'APPROVED', 100.00, false, ?, ?, ?, ?, 0)
                """, claimId, timestamp, timestamp, timestamp, timestamp);
    }

    private int count(String table, String column, String value) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + column + " = ?",
                Integer.class, value);
    }
}
