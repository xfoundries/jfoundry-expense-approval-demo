package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

import java.util.List;
import java.util.Optional;

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
import org.jfoundry.application.exception.ExternalAccessException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisExpenseClaimRepository implements ExpenseClaimRepository {

    private final ExpenseClaimMapper claimMapper;
    private final ExpenseItemMapper itemMapper;
    private final ClaimActionMapper actionMapper;

    public MybatisExpenseClaimRepository(
            ExpenseClaimMapper claimMapper,
            ExpenseItemMapper itemMapper,
            ClaimActionMapper actionMapper) {
        this.claimMapper = claimMapper;
        this.itemMapper = itemMapper;
        this.actionMapper = actionMapper;
    }

    @Override
    public Optional<ExpenseClaim> findById(ExpenseClaimId id) {
        try {
            ExpenseClaimData root = claimMapper.selectById(id.value());
            if (root == null) {
                return Optional.empty();
            }
            return Optional.of(toDomain(
                    root,
                    itemMapper.selectByClaimId(id.value()),
                    actionMapper.selectByClaimId(id.value())));
        } catch (DataAccessException exception) {
            throw new ExternalAccessException("Failed to load expense claim", exception);
        }
    }

    @Override
    public void save(ExpenseClaim claim) {
        try {
            ExpenseClaimData root = toData(claim);
            if (claimMapper.selectById(root.getId()) == null) {
                claimMapper.insert(root);
            } else {
                claimMapper.updateById(root);
            }

            itemMapper.deleteByClaimId(root.getId());
            List<ExpenseItem> items = claim.items();
            for (int index = 0; index < items.size(); index++) {
                itemMapper.insert(toData(root.getId(), index, items.get(index)));
            }

            actionMapper.deleteByClaimId(root.getId());
            List<ClaimAction> actions = claim.actions();
            for (int index = 0; index < actions.size(); index++) {
                actionMapper.insert(toData(root.getId(), index, actions.get(index)));
            }
        } catch (DataAccessException exception) {
            throw new ExternalAccessException("Failed to save expense claim", exception);
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
        data.setVersion(claim.version());
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
                root.isFinanceApprovalRequired(),
                root.getVersion());
    }
}
