package io.github.xfoundries.demo.expenseapproval.application.command;

import org.jfoundry.domain.exception.DomainRuleViolationException;

public final class MonthlyExpenseLimitExceeded extends DomainRuleViolationException {

    public MonthlyExpenseLimitExceeded(String message) {
        super(message);
    }
}
