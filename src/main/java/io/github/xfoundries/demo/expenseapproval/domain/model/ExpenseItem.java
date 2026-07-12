package io.github.xfoundries.demo.expenseapproval.domain.model;

import java.time.LocalDate;

import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jmolecules.ddd.types.Entity;

public final class ExpenseItem implements Entity<ExpenseClaim, ExpenseItemId> {

    private final ExpenseItemId id;
    private final LocalDate expenseDate;
    private final ExpenseCategory category;
    private final Money amount;
    private final String description;
    private final String receiptReference;

    public ExpenseItem(
            ExpenseItemId id,
            LocalDate expenseDate,
            ExpenseCategory category,
            Money amount,
            String description,
            String receiptReference) {
        if (id == null || expenseDate == null || category == null || amount == null) {
            throw new DomainRuleViolationException("Expense item fields are required");
        }
        if (amount.amount().signum() <= 0) {
            throw new DomainRuleViolationException("Expense amount must be positive");
        }
        if (description == null || description.isBlank()) {
            throw new DomainRuleViolationException("Expense description is required");
        }
        if (description.trim().length() > 500) {
            throw new DomainRuleViolationException("Expense description must not exceed 500 characters");
        }
        this.id = id;
        this.expenseDate = expenseDate;
        this.category = category;
        this.amount = amount;
        this.description = description.trim();
        this.receiptReference = normalizeOptional(receiptReference);
    }

    @Override
    public ExpenseItemId getId() {
        return id;
    }

    public ExpenseItemId id() {
        return id;
    }

    public LocalDate expenseDate() {
        return expenseDate;
    }

    public ExpenseCategory category() {
        return category;
    }

    public Money amount() {
        return amount;
    }

    public String description() {
        return description;
    }

    public String receiptReference() {
        return receiptReference;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

