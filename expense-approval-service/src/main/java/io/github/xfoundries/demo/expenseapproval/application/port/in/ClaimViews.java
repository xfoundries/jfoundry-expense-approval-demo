package io.github.xfoundries.demo.expenseapproval.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;

public final class ClaimViews {

    private ClaimViews() {
    }

    public record PageQuery(int page, int size) {
    }

    public record PageResult<T>(List<T> items, long total, int page, int size) {
        public PageResult {
            items = List.copyOf(items);
        }
    }

    public record ClaimSummary(
            String id,
            String claimantId,
            String title,
            ClaimState state,
            BigDecimal total,
            Instant updatedAt,
            PaymentView payment) {

        public ClaimSummary(
                String id,
                String claimantId,
                String title,
                ClaimState state,
                BigDecimal total,
                Instant updatedAt) {
            this(id, claimantId, title, state, total, updatedAt, PaymentView.forState(state));
        }
    }

    public enum PaymentDisplayStatus {
        NOT_APPLICABLE,
        PENDING,
        PAID,
        FAILED
    }

    public record PaymentView(
            PaymentDisplayStatus status,
            String paymentReference,
            String failureCode,
            String failureReason,
            Instant processedAt) {

        public static PaymentView forState(ClaimState state) {
            PaymentDisplayStatus status = state == ClaimState.APPROVED
                    ? PaymentDisplayStatus.PENDING
                    : PaymentDisplayStatus.NOT_APPLICABLE;
            return new PaymentView(status, null, null, null, null);
        }
    }

    public record ItemView(
            String id,
            LocalDate expenseDate,
            String category,
            BigDecimal amount,
            String description,
            String receiptReference) {
    }

    public record ActionView(
            String type,
            String actorId,
            Instant actedAt,
            String resultingState,
            String reason) {
    }

    public record ClaimDetail(
            String id,
            String claimantId,
            String title,
            ClaimState state,
            BigDecimal total,
            Instant createdAt,
            Instant updatedAt,
            Instant submittedAt,
            Instant completedAt,
            List<ItemView> items,
            List<ActionView> actions,
            PaymentView payment) {
        public ClaimDetail {
            items = List.copyOf(items);
            actions = List.copyOf(actions);
        }

        public ClaimDetail(
                String id,
                String claimantId,
                String title,
                ClaimState state,
                BigDecimal total,
                Instant createdAt,
                Instant updatedAt,
                Instant submittedAt,
                Instant completedAt,
                List<ItemView> items,
                List<ActionView> actions) {
            this(id, claimantId, title, state, total, createdAt, updatedAt,
                    submittedAt, completedAt, items, actions, PaymentView.forState(state));
        }
    }
}
