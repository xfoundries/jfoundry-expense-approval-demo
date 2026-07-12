package io.github.xfoundries.demo.expenseapproval.web.claim;

import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.Actor;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.ActorRole;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.springframework.stereotype.Component;

@Component
public class RequestActorResolver {

    public Actor resolve(String userId, String role) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidArgumentException("X-User-Id must not be blank");
        }
        if (role == null || role.isBlank()) {
            throw new InvalidArgumentException("X-User-Role must not be blank");
        }
        try {
            return new Actor(UserId.of(userId), ActorRole.valueOf(role.trim().toUpperCase()));
        } catch (IllegalArgumentException exception) {
            throw new InvalidArgumentException("Unknown X-User-Role: " + role, exception);
        }
    }
}

