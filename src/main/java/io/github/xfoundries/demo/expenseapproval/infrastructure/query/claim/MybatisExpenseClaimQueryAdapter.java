package io.github.xfoundries.demo.expenseapproval.infrastructure.query.claim;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ActionView;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ItemView;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.application.port.out.ExpenseClaimQueryPort;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ClaimActionData;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ClaimActionMapper;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseClaimData;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseClaimMapper;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseItemData;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseItemMapper;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.infrastructure.persistence.PersistenceFailureTranslator;
import org.jfoundry.infrastructure.persistence.PersistenceOperation;
import org.springframework.stereotype.Repository;

@Repository
@SecondaryAdapter
public class MybatisExpenseClaimQueryAdapter implements ExpenseClaimQueryPort {

    private final ExpenseClaimMapper claimMapper;
    private final ExpenseItemMapper itemMapper;
    private final ClaimActionMapper actionMapper;
    private final PersistenceFailureTranslator failureTranslator;

    public MybatisExpenseClaimQueryAdapter(
            ExpenseClaimMapper claimMapper,
            ExpenseItemMapper itemMapper,
            ClaimActionMapper actionMapper,
            PersistenceFailureTranslator failureTranslator) {
        this.claimMapper = claimMapper;
        this.itemMapper = itemMapper;
        this.actionMapper = actionMapper;
        this.failureTranslator = failureTranslator;
    }

    @Override
    public PageResult<ClaimSummary> findOwned(
            UserId claimant, ClaimState state, PageQuery page) {
        LambdaQueryWrapper<ExpenseClaimData> query = orderedQuery()
                .eq(ExpenseClaimData::getClaimantId, claimant.value());
        if (state != null) {
            query.eq(ExpenseClaimData::getState, state.name());
        }
        return findPage(query, page);
    }

    @Override
    public PageResult<ClaimSummary> findPendingManager(PageQuery page) {
        return findPage(orderedQuery().eq(
                ExpenseClaimData::getState, ClaimState.PENDING_MANAGER.name()), page);
    }

    @Override
    public PageResult<ClaimSummary> findPendingFinance(PageQuery page) {
        return findPage(orderedQuery().eq(
                ExpenseClaimData::getState, ClaimState.PENDING_FINANCE.name()), page);
    }

    @Override
    public Optional<ClaimDetail> findDetail(ExpenseClaimId id) {
        try {
            ExpenseClaimData root = claimMapper.selectById(id.value());
            if (root == null) {
                return Optional.empty();
            }
            List<ItemView> items = itemMapper.selectByClaimId(id.value()).stream()
                    .map(MybatisExpenseClaimQueryAdapter::toView)
                    .toList();
            List<ActionView> actions = actionMapper.selectByClaimId(id.value()).stream()
                    .map(MybatisExpenseClaimQueryAdapter::toView)
                    .toList();
            return Optional.of(new ClaimDetail(
                    root.getId(),
                    root.getClaimantId(),
                    root.getTitle(),
                    ClaimState.valueOf(root.getState()),
                    root.getTotalAmount(),
                    root.getCreatedAt(),
                    root.getUpdatedAt(),
                    root.getSubmittedAt(),
                    root.getCompletedAt(),
                    items,
                    actions));
        } catch (RuntimeException failure) {
            throw failureTranslator.translate(PersistenceOperation.QUERY, failure);
        }
    }

    private PageResult<ClaimSummary> findPage(
            LambdaQueryWrapper<ExpenseClaimData> query, PageQuery pageQuery) {
        try {
            Page<ExpenseClaimData> page = claimMapper.selectPage(
                    Page.of(pageQuery.page() + 1L, pageQuery.size()), query);
            return new PageResult<>(
                    page.getRecords().stream().map(MybatisExpenseClaimQueryAdapter::toSummary).toList(),
                    page.getTotal(),
                    pageQuery.page(),
                    pageQuery.size());
        } catch (RuntimeException failure) {
            throw failureTranslator.translate(PersistenceOperation.QUERY, failure);
        }
    }

    private static LambdaQueryWrapper<ExpenseClaimData> orderedQuery() {
        return new LambdaQueryWrapper<ExpenseClaimData>()
                .orderByDesc(ExpenseClaimData::getUpdatedAt)
                .orderByDesc(ExpenseClaimData::getId);
    }

    private static ClaimSummary toSummary(ExpenseClaimData data) {
        return new ClaimSummary(
                data.getId(),
                data.getClaimantId(),
                data.getTitle(),
                ClaimState.valueOf(data.getState()),
                data.getTotalAmount(),
                data.getUpdatedAt());
    }

    private static ItemView toView(ExpenseItemData data) {
        return new ItemView(
                data.getId(),
                data.getExpenseDate(),
                data.getCategory(),
                data.getAmount(),
                data.getDescription(),
                data.getReceiptReference());
    }

    private static ActionView toView(ClaimActionData data) {
        return new ActionView(
                data.getActionType(),
                data.getActorId(),
                data.getActedAt(),
                data.getResultingState(),
                data.getReason());
    }
}
