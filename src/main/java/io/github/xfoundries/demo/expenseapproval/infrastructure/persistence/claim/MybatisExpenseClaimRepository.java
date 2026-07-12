package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import org.jfoundry.infrastructure.persistence.AbstractAggregateRepository;
import org.jfoundry.infrastructure.persistence.AggregatePersistenceContext;
import org.jfoundry.infrastructure.persistence.PersistenceStateKey;
import org.jfoundry.infrastructure.persistence.mybatis.VersionedDataAccessor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisExpenseClaimRepository
        extends AbstractAggregateRepository<ExpenseClaim, ExpenseClaimId>
        implements ExpenseClaimRepository {

    private static final PersistenceStateKey<Long> VERSION =
            PersistenceStateKey.of("expense-claim.version", Long.class);
    private static final VersionedDataAccessor<ExpenseClaimData, Long> VERSION_ACCESSOR =
            new ExpenseClaimVersionAccessor();

    private final ExpenseClaimMapper claimMapper;
    private final ExpenseItemMapper itemMapper;
    private final ClaimActionMapper actionMapper;
    private final AggregatePersistenceContext persistenceContext;

    public MybatisExpenseClaimRepository(
            ExpenseClaimMapper claimMapper,
            ExpenseItemMapper itemMapper,
            ClaimActionMapper actionMapper,
            AggregatePersistenceContext persistenceContext) {
        this.claimMapper = claimMapper;
        this.itemMapper = itemMapper;
        this.actionMapper = actionMapper;
        this.persistenceContext = persistenceContext;
    }

    @Override
    protected ExpenseClaim doFindById(ExpenseClaimId id) {
        ExpenseClaimData root = claimMapper.selectById(id.value());
        if (root == null) {
            return null;
        }
        ExpenseClaim claim = toDomain(
                root,
                itemMapper.selectByClaimId(id.value()),
                actionMapper.selectByClaimId(id.value()));
        persistenceContext.attach(claim, VERSION, VERSION_ACCESSOR.getVersion(root));
        return claim;
    }

    @Override
    protected void doAdd(ExpenseClaim claim) {
        ExpenseClaimData root = toData(claim);
        try {
            claimMapper.insert(root);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException(
                    "Expense claim already exists: " + claim.id().value(), exception);
        }
        insertItems(root.getId(), claim.items());
        insertActions(root.getId(), claim.actions(), 0);
        persistenceContext.attach(claim, VERSION, VERSION_ACCESSOR.getVersion(root));
    }

    @Override
    protected void doModify(ExpenseClaim claim) {
        ExpenseClaimData root = toData(claim);
        Long originalVersion = persistenceContext.require(claim, VERSION);
        VERSION_ACCESSOR.setVersion(root, originalVersion);
        int affected = claimMapper.updateById(root);
        if (affected == 0) {
            throw new ConflictException(
                    "Expense claim optimistic lock conflict: " + claim.id().value());
        }
        itemMapper.deleteByClaimId(root.getId());
        insertItems(root.getId(), claim.items());

        int persistedActionCount = actionMapper.selectByClaimId(root.getId()).size();
        if (persistedActionCount > claim.actions().size()) {
            throw new IllegalStateException(
                    "Persisted action history is longer than aggregate history: " + claim.id().value());
        }
        insertActions(root.getId(), claim.actions(), persistedActionCount);
        persistenceContext.replace(claim, VERSION, VERSION_ACCESSOR.getVersion(root));
    }

    @Override
    protected void doRemove(ExpenseClaim claim) {
        Long originalVersion = persistenceContext.require(claim, VERSION);
        int affected = claimMapper.delete(Wrappers.<ExpenseClaimData>lambdaQuery()
                .eq(ExpenseClaimData::getId, claim.id().value())
                .eq(ExpenseClaimData::getVersion, originalVersion));
        if (affected == 0) {
            throw new ConflictException(
                    "Expense claim remove optimistic lock conflict: " + claim.id().value());
        }
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

    private static ExpenseClaimData toData(ExpenseClaim claim) {
        ExpenseClaimData data = new ExpenseClaimData();
        data.setId(claim.id().value());
        data.setClaimantId(claim.claimant().value());
        data.setTitle(claim.title());
        data.setState(claim.state().name());
        data.setTotalAmount(claim.total().amount());
        data.setFinanceApprovalRequired(claim.financeApprovalRequired());
        data.setCreatedAt(claim.createdAt());
        data.setUpdatedAt(claim.updatedAt());
        data.setSubmittedAt(claim.submittedAt().orElse(null));
        data.setCompletedAt(claim.completedAt().orElse(null));
        return data;
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

    private static final class ExpenseClaimVersionAccessor
            implements VersionedDataAccessor<ExpenseClaimData, Long> {

        @Override
        public Class<Long> versionType() {
            return Long.class;
        }

        @Override
        public Long getVersion(ExpenseClaimData data) {
            return data.getVersion();
        }

        @Override
        public void setVersion(ExpenseClaimData data, Long version) {
            data.setVersion(version);
        }
    }
}
