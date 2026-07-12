package io.github.xfoundries.demo.expenseapproval.domain.model;

import org.jmolecules.ddd.types.ValueObject;

public record UserId(String value) implements ValueObject {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("User id must not be blank");
        }
        value = value.trim();
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}

