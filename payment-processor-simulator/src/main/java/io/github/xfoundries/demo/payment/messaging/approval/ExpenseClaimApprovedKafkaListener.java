package io.github.xfoundries.demo.payment.messaging.approval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ExpenseClaimApprovedV1;
import io.github.xfoundries.demo.payment.application.ExpenseClaimApprovedProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExpenseClaimApprovedKafkaListener {

    private final ObjectMapper objectMapper;
    private final ExpenseClaimApprovedProcessor processor;

    public ExpenseClaimApprovedKafkaListener(
            ObjectMapper objectMapper,
            ExpenseClaimApprovedProcessor processor) {
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @KafkaListener(
            topics = "${payment-simulator.messaging.approvals-topic:expense-approval.events.v1}",
            groupId = ExpenseClaimApprovedProcessor.CONSUMER_NAME,
            autoStartup = "${payment-simulator.messaging.kafka-listener-enabled:false}")
    public void onMessage(String json) throws Exception {
        EventEnvelope<ExpenseClaimApprovedV1> event = objectMapper.readValue(
                json, new TypeReference<EventEnvelope<ExpenseClaimApprovedV1>>() {
                });
        processor.process(event);
    }
}
