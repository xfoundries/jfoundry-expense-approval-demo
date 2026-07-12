package io.github.xfoundries.demo.expenseapproval.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseCategory;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;

public final class ClaimCommands {

    private ClaimCommands() {
    }

    public enum ActorRole {
        EMPLOYEE,
        MANAGER,
        FINANCE
    }

    public record Actor(UserId userId, ActorRole role) {
        public Actor {
            if (userId == null || role == null) {
                throw new IllegalArgumentException("Actor user id and role are required");
            }
        }
    }

    public record CreateClaimCommand(Actor actor, String title) {
    }

    public record AddItemCommand(
            Actor actor,
            ExpenseClaimId claimId,
            LocalDate expenseDate,
            ExpenseCategory category,
            BigDecimal amount,
            String description,
            String receiptReference) {
    }

    public record UpdateItemCommand(
            Actor actor,
            ExpenseClaimId claimId,
            ExpenseItemId itemId,
            LocalDate expenseDate,
            ExpenseCategory category,
            BigDecimal amount,
            String description,
            String receiptReference) {
    }

    public record RemoveItemCommand(
            Actor actor, ExpenseClaimId claimId, ExpenseItemId itemId) {
    }

    public record ClaimCommand(Actor actor, ExpenseClaimId claimId) {
    }

    public record ApproveCommand(Actor actor, ExpenseClaimId claimId) {
    }

    public record RejectClaimCommand(Actor actor, ExpenseClaimId claimId, String reason) {
    }
}

