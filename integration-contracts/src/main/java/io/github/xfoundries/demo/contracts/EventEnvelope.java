package io.github.xfoundries.demo.contracts;

import java.time.Instant;
import java.util.Objects;

public record EventEnvelope<T>(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String aggregateId,
        T payload) {

    public EventEnvelope {
        requireText(eventId, "eventId");
        requireText(eventType, "eventType");
        if (eventVersion <= 0) {
            throw new IllegalArgumentException("eventVersion must be positive");
        }
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        requireText(correlationId, "correlationId");
        if (causationId != null) {
            requireText(causationId, "causationId");
        }
        requireText(aggregateId, "aggregateId");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public static <T> EventEnvelope<T> create(
            String eventId,
            String eventType,
            int eventVersion,
            Instant occurredAt,
            String correlationId,
            String causationId,
            String aggregateId,
            T payload) {
        return new EventEnvelope<>(
                eventId,
                eventType,
                eventVersion,
                occurredAt,
                correlationId,
                causationId,
                aggregateId,
                payload);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
