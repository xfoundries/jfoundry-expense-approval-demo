package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

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
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import org.jfoundry.application.exception.ConflictException;
import org.jfoundry.infrastructure.persistence.mybatis.MybatisPlusAggregateRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisExpenseClaimRepository
        extends MybatisPlusAggregateRepository<
                ExpenseClaim, ExpenseClaimId, ExpenseClaimData, String>
        implements ExpenseClaimRepository {

    private static final ExpenseClaimRootDataMapper ROOT_DATA_MAPPER =
            new ExpenseClaimRootDataMapper();

    private final ExpenseItemMapper itemMapper;
    private final ClaimActionMapper actionMapper;

    public MybatisExpenseClaimRepository(
            ExpenseClaimMapper claimMapper,
            ExpenseItemMapper itemMapper,
            ClaimActionMapper actionMapper) {
        super(
                claimMapper,
                ROOT_DATA_MAPPER::toData,
                ROOT_DATA_MAPPER::toDataId,
                ExpenseClaimData.class);
        this.itemMapper = itemMapper;
        this.actionMapper = actionMapper;
    }

    @Override
    protected ExpenseClaim doFindById(ExpenseClaimId id) {
        return loadAggregate(id, root -> toDomain(
                root,
                itemMapper.selectByClaimId(root.getId()),
                actionMapper.selectByClaimId(root.getId())));
    }

    @Override
    protected void doAdd(ExpenseClaim claim) {
        insertAggregate(
                claim,
                ROOT_DATA_MAPPER.toData(claim),
                root -> {
                    insertItems(root.getId(), claim.items());
                    insertActions(root.getId(), claim.actions(), 0);
                },
                failure -> translateRootInsertFailure(claim, failure));
    }

    @Override
    protected void doModify(ExpenseClaim claim) {
        updateAggregate(claim, ROOT_DATA_MAPPER.toData(claim), root -> {
            itemMapper.deleteByClaimId(root.getId());
            insertItems(root.getId(), claim.items());

            int persistedActionCount = actionMapper.selectByClaimId(root.getId()).size();
            if (persistedActionCount > claim.actions().size()) {
                throw new IllegalStateException(
                        "Persisted action history is longer than aggregate history: "
                                + claim.id().value());
            }
            insertActions(root.getId(), claim.actions(), persistedActionCount);
        });
    }

    private void insertItems(String claimId, List<ExpenseItem> items) {
        for (int index = 0; index < items.size(); index++) {
            itemMapper.insert(toData(claimId, index, items.get(index)));
        }
    }

    private void insertActions(String claimId, List<ClaimAction> actions, int startIndex) {
        for (int index = startIndex; index < actions.size(); index++) {
            actionMapper.insert(toData(claimId, index, actions.get(index)));
        }
    }

    private static ExpenseItemData toData(String claimId, int order, ExpenseItem item) {
        ExpenseItemData data = new ExpenseItemData();
        data.setId(item.id().value());
        data.setClaimId(claimId);
        data.setItemOrder(order);
        data.setExpenseDate(item.expenseDate());
        data.setCategory(item.category().name());
        data.setAmount(item.amount().amount());
        data.setDescription(item.description());
        data.setReceiptReference(item.receiptReference());
        return data;
    }

    private static ClaimActionData toData(String claimId, int sequence, ClaimAction action) {
        ClaimActionData data = new ClaimActionData();
        data.setId(claimId + ":" + sequence);
        data.setClaimId(claimId);
        data.setSequenceNo(sequence);
        data.setActionType(action.type().name());
        data.setActorId(action.actor().value());
        data.setActedAt(action.actedAt());
        data.setResultingState(action.resultingState().name());
        data.setReason(action.reason().orElse(null));
        return data;
    }

    private static ExpenseClaim toDomain(
            ExpenseClaimData root,
            List<ExpenseItemData> itemData,
            List<ClaimActionData> actionData) {
        List<ExpenseItem> items = itemData.stream()
                .map(data -> new ExpenseItem(
                        ExpenseItemId.of(data.getId()),
                        data.getExpenseDate(),
                        ExpenseCategory.valueOf(data.getCategory()),
                        Money.positiveCny(data.getAmount()),
                        data.getDescription(),
                        data.getReceiptReference()))
                .toList();
        List<ClaimAction> actions = actionData.stream()
                .map(data -> ClaimAction.of(
                        ClaimActionType.valueOf(data.getActionType()),
                        UserId.of(data.getActorId()),
                        data.getActedAt(),
                        ClaimState.valueOf(data.getResultingState()),
                        data.getReason()))
                .toList();
        return ExpenseClaim.restore(
                ExpenseClaimId.of(root.getId()),
                UserId.of(root.getClaimantId()),
                root.getTitle(),
                ClaimState.valueOf(root.getState()),
                items,
                actions,
                root.getCreatedAt(),
                root.getUpdatedAt(),
                root.getSubmittedAt(),
                root.getCompletedAt(),
                root.isFinanceApprovalRequired());
    }

    private static RuntimeException translateRootInsertFailure(
            ExpenseClaim claim,
            RuntimeException failure) {
        if (failure instanceof DuplicateKeyException duplicateKey) {
            return new ConflictException(
                    "Expense claim already exists: " + claim.id().value(), duplicateKey);
        }
        return failure;
    }
}
