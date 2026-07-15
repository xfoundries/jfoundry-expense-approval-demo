package io.github.xfoundries.demo.expenseapproval.infrastructure.query.claim;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ActionView;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.ItemView;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PaymentDisplayStatus;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PaymentView;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ExpenseClaimViewReader;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ClaimActionData;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ClaimActionMapper;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseClaimData;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseClaimMapper;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseItemData;
import io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim.ExpenseItemMapper;
import io.github.xfoundries.demo.expenseapproval.infrastructure.projection.payment.PaymentStatusData;
import io.github.xfoundries.demo.expenseapproval.infrastructure.projection.payment.PaymentStatusMapper;
import org.jfoundry.infrastructure.persistence.AbstractPersistenceAdapter;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisExpenseClaimViewReader
        extends AbstractPersistenceAdapter
        implements ExpenseClaimViewReader {

    private final ExpenseClaimMapper claimMapper;
    private final ExpenseItemMapper itemMapper;
    private final ClaimActionMapper actionMapper;
    private final PaymentStatusMapper paymentStatusMapper;

    public MybatisExpenseClaimViewReader(
            ExpenseClaimMapper claimMapper,
            ExpenseItemMapper itemMapper,
            ClaimActionMapper actionMapper,
            PaymentStatusMapper paymentStatusMapper) {
        this.claimMapper = claimMapper;
        this.itemMapper = itemMapper;
        this.actionMapper = actionMapper;
        this.paymentStatusMapper = paymentStatusMapper;
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
        return query(() -> {
            ExpenseClaimData root = claimMapper.selectById(id.value());
            if (root == null) {
                return Optional.empty();
            }
            List<ItemView> items = itemMapper.selectByClaimId(id.value()).stream()
                    .map(MybatisExpenseClaimViewReader::toView)
                    .toList();
            List<ActionView> actions = actionMapper.selectByClaimId(id.value()).stream()
                    .map(MybatisExpenseClaimViewReader::toView)
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
                    actions,
                    toPayment(ClaimState.valueOf(root.getState()), paymentStatusMapper.selectById(root.getId()))));
        });
    }

    private PageResult<ClaimSummary> findPage(
            LambdaQueryWrapper<ExpenseClaimData> query, PageQuery pageQuery) {
        return query(() -> {
            Page<ExpenseClaimData> page = claimMapper.selectPage(
                    Page.of(pageQuery.page() + 1L, pageQuery.size()), query);
            Map<String, PaymentStatusData> payments = findPayments(page.getRecords());
            return new PageResult<>(
                    page.getRecords().stream()
                            .map(data -> toSummary(data, payments.get(data.getId())))
                            .toList(),
                    page.getTotal(),
                    pageQuery.page(),
                    pageQuery.size());
        });
    }

    private Map<String, PaymentStatusData> findPayments(List<ExpenseClaimData> claims) {
        if (claims.isEmpty()) {
            return Map.of();
        }
        List<String> claimIds = claims.stream().map(ExpenseClaimData::getId).toList();
        return paymentStatusMapper.selectList(
                        new LambdaQueryWrapper<PaymentStatusData>()
                                .in(PaymentStatusData::getClaimId, claimIds)).stream()
                .collect(Collectors.toMap(PaymentStatusData::getClaimId, Function.identity()));
    }

    private static LambdaQueryWrapper<ExpenseClaimData> orderedQuery() {
        return new LambdaQueryWrapper<ExpenseClaimData>()
                .orderByDesc(ExpenseClaimData::getUpdatedAt)
                .orderByDesc(ExpenseClaimData::getId);
    }

    private static ClaimSummary toSummary(ExpenseClaimData data, PaymentStatusData payment) {
        ClaimState state = ClaimState.valueOf(data.getState());
        return new ClaimSummary(
                data.getId(),
                data.getClaimantId(),
                data.getTitle(),
                state,
                data.getTotalAmount(),
                data.getUpdatedAt(),
                toPayment(state, payment));
    }

    private static PaymentView toPayment(ClaimState claimState, PaymentStatusData payment) {
        if (claimState != ClaimState.APPROVED) {
            return PaymentView.forState(claimState);
        }
        if (payment == null) {
            return PaymentView.forState(claimState);
        }
        return new PaymentView(
                PaymentDisplayStatus.valueOf(payment.getStatus()),
                payment.getPaymentReference(),
                payment.getFailureCode(),
                payment.getFailureReason(),
                payment.getProcessedAt());
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
