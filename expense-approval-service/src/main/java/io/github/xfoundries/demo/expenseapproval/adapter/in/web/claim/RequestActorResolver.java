package io.github.xfoundries.demo.expenseapproval.adapter.in.web.claim;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.springframework.stereotype.Component;

@Component
public class RequestActorResolver {

    public ApprovalActor resolve(String userId, String role) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidArgumentException("X-User-Id must not be blank");
        }
        if (role == null || role.isBlank()) {
            throw new InvalidArgumentException("X-User-Role must not be blank");
        }
        try {
            return new ApprovalActor(UserId.of(userId), ApprovalRole.valueOf(role.trim().toUpperCase()));
        } catch (IllegalArgumentException exception) {
            throw new InvalidArgumentException("Unknown X-User-Role: " + role, exception);
        }
    }
}
