package io.github.xfoundries.demo.expenseapproval.application.command;

import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;

public record Actor(UserId userId, ActorRole role) {

    public Actor {
        if (userId == null || role == null) {
            throw new IllegalArgumentException("Actor user id and role are required");
        }
    }
}
