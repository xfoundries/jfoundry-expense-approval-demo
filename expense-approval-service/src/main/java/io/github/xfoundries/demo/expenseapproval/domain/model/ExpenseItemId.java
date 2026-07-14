package io.github.xfoundries.demo.expenseapproval.domain.model;

import java.util.UUID;

import org.jmolecules.ddd.types.Identifier;

public record ExpenseItemId(String value) implements Identifier {

    public ExpenseItemId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Expense item id must not be blank");
        }
    }

    public static ExpenseItemId of(String value) {
        return new ExpenseItemId(value);
    }

    public static ExpenseItemId generate() {
        return of(UUID.randomUUID().toString());
    }
}

