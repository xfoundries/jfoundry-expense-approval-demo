package io.github.xfoundries.demo.expenseapproval.domain.model;

import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jmolecules.ddd.types.ValueObject;

public record RejectionReason(String value) implements ValueObject {

    public RejectionReason {
        if (value == null || value.isBlank()) {
            throw new DomainRuleViolationException("Rejection reason is required");
        }
        value = value.trim();
        if (value.length() > 500) {
            throw new DomainRuleViolationException("Rejection reason must not exceed 500 characters");
        }
    }

    public static RejectionReason of(String value) {
        return new RejectionReason(value);
    }
}

