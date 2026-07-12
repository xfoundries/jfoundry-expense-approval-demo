package io.github.xfoundries.demo.expenseapproval.domain.model;

import java.util.UUID;

import org.jmolecules.ddd.types.Identifier;

public record ExpenseClaimId(String value) implements Identifier {

    public ExpenseClaimId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Expense claim id must not be blank");
        }
    }

    public static ExpenseClaimId of(String value) {
        return new ExpenseClaimId(value);
    }

    public static ExpenseClaimId generate() {
        return of(UUID.randomUUID().toString());
    }
}

