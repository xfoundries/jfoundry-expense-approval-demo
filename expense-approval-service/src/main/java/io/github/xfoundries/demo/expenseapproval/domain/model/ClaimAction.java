package io.github.xfoundries.demo.expenseapproval.domain.model;

import java.time.Instant;
import java.util.Optional;

import org.jmolecules.ddd.types.ValueObject;

public record ClaimAction(
        ClaimActionType type,
        UserId actor,
        Instant actedAt,
        ClaimState resultingState,
        Optional<String> reason) implements ValueObject {

    public ClaimAction {
        if (type == null || actor == null || actedAt == null || resultingState == null) {
            throw new IllegalArgumentException("Claim action fields are required");
        }
        reason = reason == null ? Optional.empty() : reason;
    }

    public static ClaimAction of(
            ClaimActionType type,
            UserId actor,
            Instant actedAt,
            ClaimState resultingState,
            String reason) {
        return new ClaimAction(type, actor, actedAt, resultingState, Optional.ofNullable(reason));
    }
}

