package io.github.xfoundries.demo.expenseapproval.messaging.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ReimbursementPaidV1;
import io.github.xfoundries.demo.contracts.ReimbursementPaymentFailedV1;
import io.github.xfoundries.demo.expenseapproval.application.integration.PaymentResultProjector;
import org.jfoundry.architecture.hexagonal.PrimaryAdapter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@PrimaryAdapter
public class PaymentResultKafkaListener {

    private final ObjectMapper objectMapper;
    private final PaymentResultProjector projector;

    public PaymentResultKafkaListener(ObjectMapper objectMapper, PaymentResultProjector projector) {
        this.objectMapper = objectMapper;
        this.projector = projector;
    }

    @KafkaListener(
            topics = "${expense-approval.messaging.payment-results-topic:payment.events.v1}",
            groupId = PaymentResultProjector.CONSUMER_NAME,
            autoStartup = "${expense-approval.messaging.kafka-listener-enabled:false}")
    public void onMessage(String json) throws Exception {
        JsonNode envelope = objectMapper.readTree(json);
        String eventType = envelope.path("eventType").asText();
        if ("ReimbursementPaid".equals(eventType)) {
            projector.projectPaid(objectMapper.readValue(
                    json, new TypeReference<EventEnvelope<ReimbursementPaidV1>>() {
                    }));
            return;
        }
        if ("ReimbursementPaymentFailed".equals(eventType)) {
            projector.projectFailed(objectMapper.readValue(
                    json, new TypeReference<EventEnvelope<ReimbursementPaymentFailedV1>>() {
                    }));
            return;
        }
        throw new IllegalArgumentException("Unsupported payment event type: " + eventType);
    }
}
