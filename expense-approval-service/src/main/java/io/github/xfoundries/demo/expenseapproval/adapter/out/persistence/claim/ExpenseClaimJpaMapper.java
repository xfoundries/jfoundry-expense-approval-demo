package io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim;

import java.util.List;

import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimAction;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimActionType;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseCategory;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import org.jfoundry.infrastructure.persistence.jpa.JpaAggregateMapper;

final class ExpenseClaimJpaMapper
        implements JpaAggregateMapper<ExpenseClaim, ExpenseClaimId, ExpenseClaimEntity, String> {

    @Override
    public String toEntityId(ExpenseClaimId id) {
        return id.value();
    }

    @Override
    public ExpenseClaimEntity newEntity(ExpenseClaim claim) {
        ExpenseClaimEntity entity = new ExpenseClaimEntity(
                claim.id().value(),
                claim.claimant().value(),
                claim.title(),
                claim.state().name(),
                claim.total().amount(),
                claim.financeApprovalRequired(),
                claim.createdAt(),
                claim.updatedAt(),
                claim.submittedAt().orElse(null),
                claim.completedAt().orElse(null));
        replaceItems(entity, claim.items());
        appendActions(entity, claim.actions(), 0);
        return entity;
    }

    @Override
    public ExpenseClaim toAggregate(ExpenseClaimEntity entity) {
        List<ExpenseItem> items = entity.items().stream()
                .map(ExpenseClaimJpaMapper::toDomain)
                .toList();
        List<ClaimAction> actions = entity.actions().stream()
                .map(ExpenseClaimJpaMapper::toDomain)
                .toList();
        return ExpenseClaim.restore(
                ExpenseClaimId.of(entity.id()),
                UserId.of(entity.claimantId()),
                entity.title(),
                ClaimState.valueOf(entity.state()),
                items,
                actions,
                entity.createdAt(),
                entity.updatedAt(),
                entity.submittedAt(),
                entity.completedAt(),
                entity.financeApprovalRequired());
    }

    @Override
    public void apply(ExpenseClaim claim, ExpenseClaimEntity entity) {
        entity.apply(
                claim.title(),
                claim.state().name(),
                claim.total().amount(),
                claim.financeApprovalRequired(),
                claim.updatedAt(),
                claim.submittedAt().orElse(null),
                claim.completedAt().orElse(null));
        replaceItems(entity, claim.items());

        int persistedActionCount = entity.actions().size();
        if (persistedActionCount > claim.actions().size()) {
            throw new IllegalStateException(
                    "Persisted action history is longer than aggregate history: " + claim.id().value());
        }
        appendActions(entity, claim.actions(), persistedActionCount);
    }

    private static void replaceItems(ExpenseClaimEntity entity, List<ExpenseItem> items) {
        entity.replaceItems(java.util.stream.IntStream.range(0, items.size())
                .mapToObj(index -> toEntity(items.get(index), index))
                .toList());
    }

    private static void appendActions(
            ExpenseClaimEntity entity, List<ClaimAction> actions, int startIndex) {
        for (int index = startIndex; index < actions.size(); index++) {
            entity.addAction(toEntity(entity.id(), index, actions.get(index)));
        }
    }

    private static ExpenseItemEntity toEntity(ExpenseItem item, int itemOrder) {
        return new ExpenseItemEntity(
                item.id().value(),
                itemOrder,
                item.expenseDate(),
                item.category().name(),
                item.amount().amount(),
                item.description(),
                item.receiptReference());
    }

    private static ClaimActionEntity toEntity(String claimId, int sequenceNo, ClaimAction action) {
        return new ClaimActionEntity(
                claimId + ":" + sequenceNo,
                sequenceNo,
                action.type().name(),
                action.actor().value(),
                action.actedAt(),
                action.resultingState().name(),
                action.reason().orElse(null));
    }

    private static ExpenseItem toDomain(ExpenseItemEntity entity) {
        return new ExpenseItem(
                ExpenseItemId.of(entity.id()),
                entity.expenseDate(),
                ExpenseCategory.valueOf(entity.category()),
                Money.positiveCny(entity.amount()),
                entity.description(),
                entity.receiptReference());
    }

    private static ClaimAction toDomain(ClaimActionEntity entity) {
        return ClaimAction.of(
                ClaimActionType.valueOf(entity.actionType()),
                UserId.of(entity.actorId()),
                entity.actedAt(),
                ClaimState.valueOf(entity.resultingState()),
                entity.reason());
    }
}
