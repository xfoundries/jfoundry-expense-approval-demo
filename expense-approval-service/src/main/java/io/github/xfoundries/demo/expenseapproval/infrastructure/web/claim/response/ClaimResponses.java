package io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ActionView;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ItemView;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PaymentDisplayStatus;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PaymentView;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;

public final class ClaimResponses {

    private ClaimResponses() {
    }

    public record PageResponse<T>(List<T> items, long total, int page, int size) {
        public PageResponse {
            items = List.copyOf(items);
        }
    }

    public record SummaryResponse(
            String id,
            String claimantId,
            String title,
            ClaimState state,
            BigDecimal total,
            Instant updatedAt,
            PaymentResponse payment) {
    }

    public record ItemResponse(
            String id,
            LocalDate expenseDate,
            String category,
            BigDecimal amount,
            String description,
            String receiptReference) {
    }

    public record ActionResponse(
            String type,
            String actorId,
            Instant actedAt,
            String resultingState,
            String reason) {
    }

    public record PaymentResponse(
            PaymentDisplayStatus status,
            String paymentReference,
            String failureCode,
            String failureReason,
            Instant processedAt) {
    }

    public record DetailResponse(
            String id,
            String claimantId,
            String title,
            ClaimState state,
            BigDecimal total,
            Instant createdAt,
            Instant updatedAt,
            Instant submittedAt,
            Instant completedAt,
            List<ItemResponse> items,
            List<ActionResponse> actions,
            PaymentResponse payment) {
    }

    public static PageResponse<SummaryResponse> from(PageResult<ClaimSummary> page) {
        return new PageResponse<>(
                page.items().stream().map(ClaimResponses::from).toList(),
                page.total(), page.page(), page.size());
    }

    public static DetailResponse from(ClaimDetail detail) {
        return new DetailResponse(
                detail.id(), detail.claimantId(), detail.title(), detail.state(), detail.total(),
                detail.createdAt(), detail.updatedAt(), detail.submittedAt(), detail.completedAt(),
                detail.items().stream().map(ClaimResponses::from).toList(),
                detail.actions().stream().map(ClaimResponses::from).toList(),
                from(detail.payment()));
    }

    private static SummaryResponse from(ClaimSummary summary) {
        return new SummaryResponse(
                summary.id(), summary.claimantId(), summary.title(), summary.state(),
                summary.total(), summary.updatedAt(), from(summary.payment()));
    }

    private static PaymentResponse from(PaymentView payment) {
        return new PaymentResponse(
                payment.status(), payment.paymentReference(), payment.failureCode(),
                payment.failureReason(), payment.processedAt());
    }

    private static ItemResponse from(ItemView item) {
        return new ItemResponse(
                item.id(), item.expenseDate(), item.category(), item.amount(),
                item.description(), item.receiptReference());
    }

    private static ActionResponse from(ActionView action) {
        return new ActionResponse(
                action.type(), action.actorId(), action.actedAt(),
                action.resultingState(), action.reason());
    }
}
