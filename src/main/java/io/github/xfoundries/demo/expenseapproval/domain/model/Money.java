package io.github.xfoundries.demo.expenseapproval.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jmolecules.ddd.types.ValueObject;

public record Money(BigDecimal amount) implements Comparable<Money>, ValueObject {

    public Money {
        if (amount == null) {
            throw new DomainRuleViolationException("Money amount is required");
        }
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new DomainRuleViolationException("CNY amount supports at most two decimal places", exception);
        }
        if (amount.signum() < 0) {
            throw new DomainRuleViolationException("Money amount must not be negative");
        }
    }

    public static Money cny(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money positiveCny(BigDecimal amount) {
        Money money = new Money(amount);
        if (money.amount.signum() <= 0) {
            throw new DomainRuleViolationException("Expense amount must be positive");
        }
        return money;
    }

    public static Money zeroCny() {
        return cny("0.00");
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }
}

