package io.github.xfoundries.demo.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ExpenseClaimApprovedV1;
import io.github.xfoundries.demo.contracts.ReimbursementPaidV1;
import io.github.xfoundries.demo.contracts.ReimbursementPaymentFailedV1;
import org.jfoundry.application.inbox.InboxTemplate;
import org.jfoundry.application.outbox.OutboxAppendRequest;
import org.jfoundry.application.outbox.OutboxTemplate;

public final class ExpenseClaimApprovedProcessor {

    public static final String CONSUMER_NAME = "payment-simulator.expense-claim-approved.v1";

    private static final String RESULT_TOPIC = "payment.events.v1";

    private final InboxTemplate inboxTemplate;
    private final OutboxTemplate outboxTemplate;
    private final PaymentRule paymentRule;
    private final Clock clock;
    private final Supplier<String> eventIds;

    public ExpenseClaimApprovedProcessor(
            InboxTemplate inboxTemplate,
            OutboxTemplate outboxTemplate,
            PaymentRule paymentRule,
            Clock clock,
            Supplier<String> eventIds) {
        this.inboxTemplate = Objects.requireNonNull(inboxTemplate);
        this.outboxTemplate = Objects.requireNonNull(outboxTemplate);
        this.paymentRule = Objects.requireNonNull(paymentRule);
        this.clock = Objects.requireNonNull(clock);
        this.eventIds = Objects.requireNonNull(eventIds);
    }

    public boolean process(EventEnvelope<ExpenseClaimApprovedV1> approval) throws Exception {
        Objects.requireNonNull(approval);
        return inboxTemplate.executeOnce(
                approval.eventId(), CONSUMER_NAME, () -> appendPaymentResult(approval));
    }

    private void appendPaymentResult(EventEnvelope<ExpenseClaimApprovedV1> approval) {
        ExpenseClaimApprovedV1 payload = approval.payload();
        PaymentDecision decision = paymentRule.decide(payload.approvedAmount(), payload.currency());
        Instant processedAt = clock.instant();
        String eventId = eventIds.get();
        if (decision.successful()) {
            EventEnvelope<ReimbursementPaidV1> result = resultEnvelope(
                    approval,
                    eventId,
                    "ReimbursementPaid",
                    new ReimbursementPaidV1(
                            payload.claimId(), "PAY-" + payload.claimId(), processedAt),
                    processedAt);
            append(eventId, payload.claimId(), "ReimbursementPaidV1", result, processedAt);
            return;
        }
        EventEnvelope<ReimbursementPaymentFailedV1> result = resultEnvelope(
                approval,
                eventId,
                "ReimbursementPaymentFailed",
                new ReimbursementPaymentFailedV1(
                        payload.claimId(), decision.failureCode(), decision.failureReason(), processedAt),
                processedAt);
        append(eventId, payload.claimId(), "ReimbursementPaymentFailedV1", result, processedAt);
    }

    private void append(
            String eventId,
            String claimId,
            String payloadType,
            Object payload,
            Instant occurredAt) {
        outboxTemplate.append(OutboxAppendRequest.of(
                eventId, RESULT_TOPIC, claimId, payloadType, payload, occurredAt));
    }

    private static <T> EventEnvelope<T> resultEnvelope(
            EventEnvelope<ExpenseClaimApprovedV1> approval,
            String eventId,
            String eventType,
            T payload,
            Instant occurredAt) {
        return EventEnvelope.create(
                eventId,
                eventType,
                1,
                occurredAt,
                approval.correlationId(),
                approval.eventId(),
                approval.aggregateId(),
                payload);
    }
}
