package io.github.xfoundries.demo.expenseapproval.application.command;

import org.jfoundry.architecture.cqrs.Command;

@Command
public record CreateExpenseClaimCommand(Actor actor, String title) {
}
