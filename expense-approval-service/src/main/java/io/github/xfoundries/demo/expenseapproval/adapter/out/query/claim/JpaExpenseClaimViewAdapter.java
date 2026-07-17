package io.github.xfoundries.demo.expenseapproval.adapter.out.query.claim;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim.ClaimActionEntity;
import io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim.ExpenseClaimEntity;
import io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim.ExpenseItemEntity;
import io.github.xfoundries.demo.expenseapproval.adapter.out.projection.payment.PaymentStatusEntity;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.port.out.ExpenseClaimViewPort;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ActionView;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ItemView;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PaymentDisplayStatus;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PaymentView;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jfoundry.architecture.hexagonal.SecondaryAdapter;
import org.jfoundry.infrastructure.persistence.AbstractPersistenceAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@SecondaryAdapter
@Component
@Transactional(readOnly = true)
public class JpaExpenseClaimViewAdapter
        extends AbstractPersistenceAdapter
        implements ExpenseClaimViewPort {

    private final EntityManager entityManager;

    public JpaExpenseClaimViewAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public PageResult<ClaimSummary> findOwned(
            UserId claimant, ClaimState state, PageQuery page) {
        return query(() -> {
            String predicate = "c.claimantId = :claimant";
            if (state != null) {
                predicate += " and c.state = :state";
            }
            TypedQuery<ExpenseClaimEntity> claims = entityManager.createQuery(
                    "select c from ExpenseClaimEntity c where " + predicate
                            + " order by c.updatedAt desc, c.id desc",
                    ExpenseClaimEntity.class);
            TypedQuery<Long> count = entityManager.createQuery(
                    "select count(c) from ExpenseClaimEntity c where " + predicate,
                    Long.class);
            claims.setParameter("claimant", claimant.value());
            count.setParameter("claimant", claimant.value());
            if (state != null) {
                claims.setParameter("state", state.name());
                count.setParameter("state", state.name());
            }
            return page(claims, count.getSingleResult(), page);
        });
    }

    @Override
    public PageResult<ClaimSummary> findPendingManager(PageQuery page) {
        return findPending(ClaimState.PENDING_MANAGER, page);
    }

    @Override
    public PageResult<ClaimSummary> findPendingFinance(PageQuery page) {
        return findPending(ClaimState.PENDING_FINANCE, page);
    }

    @Override
    public Optional<ClaimDetail> findDetail(ExpenseClaimId id) {
        return find(() -> Optional.ofNullable(entityManager.find(ExpenseClaimEntity.class, id.value()))
                .map(entity -> new ClaimDetail(
                        entity.id(),
                        entity.claimantId(),
                        entity.title(),
                        ClaimState.valueOf(entity.state()),
                        entity.totalAmount(),
                        entity.createdAt(),
                        entity.updatedAt(),
                        entity.submittedAt(),
                        entity.completedAt(),
                        entity.items().stream().map(JpaExpenseClaimViewAdapter::toView).toList(),
                        entity.actions().stream().map(JpaExpenseClaimViewAdapter::toView).toList(),
                        toPayment(ClaimState.valueOf(entity.state()), entityManager.find(
                                PaymentStatusEntity.class, entity.id())))));
    }

    private PageResult<ClaimSummary> findPending(ClaimState state, PageQuery page) {
        return query(() -> {
            TypedQuery<ExpenseClaimEntity> claims = entityManager.createQuery(
                    "select c from ExpenseClaimEntity c where c.state = :state"
                            + " order by c.updatedAt desc, c.id desc",
                    ExpenseClaimEntity.class);
            TypedQuery<Long> count = entityManager.createQuery(
                    "select count(c) from ExpenseClaimEntity c where c.state = :state",
                    Long.class);
            claims.setParameter("state", state.name());
            count.setParameter("state", state.name());
            return page(claims, count.getSingleResult(), page);
        });
    }

    private PageResult<ClaimSummary> page(
            TypedQuery<ExpenseClaimEntity> query, long total, PageQuery page) {
        List<ExpenseClaimEntity> claims = query
                .setFirstResult(page.page() * page.size())
                .setMaxResults(page.size())
                .getResultList();
        Map<String, PaymentStatusEntity> payments = findPayments(claims);
        return new PageResult<>(
                claims.stream()
                        .map(claim -> toSummary(claim, payments.get(claim.id())))
                        .toList(),
                total,
                page.page(),
                page.size());
    }

    private Map<String, PaymentStatusEntity> findPayments(List<ExpenseClaimEntity> claims) {
        if (claims.isEmpty()) {
            return Map.of();
        }
        return entityManager.createQuery(
                        "select p from PaymentStatusEntity p where p.claimId in :claimIds",
                        PaymentStatusEntity.class)
                .setParameter("claimIds", claims.stream().map(ExpenseClaimEntity::id).toList())
                .getResultList().stream()
                .collect(Collectors.toMap(PaymentStatusEntity::claimId, Function.identity()));
    }

    private static ClaimSummary toSummary(
            ExpenseClaimEntity entity, PaymentStatusEntity payment) {
        ClaimState state = ClaimState.valueOf(entity.state());
        return new ClaimSummary(
                entity.id(),
                entity.claimantId(),
                entity.title(),
                state,
                entity.totalAmount(),
                entity.updatedAt(),
                toPayment(state, payment));
    }

    private static PaymentView toPayment(ClaimState claimState, PaymentStatusEntity payment) {
        if (claimState != ClaimState.APPROVED || payment == null) {
            return PaymentView.forState(claimState);
        }
        return new PaymentView(
                PaymentDisplayStatus.valueOf(payment.status()),
                payment.paymentReference(),
                payment.failureCode(),
                payment.failureReason(),
                payment.processedAt());
    }

    private static ItemView toView(ExpenseItemEntity entity) {
        return new ItemView(
                entity.id(),
                entity.expenseDate(),
                entity.category(),
                entity.amount(),
                entity.description(),
                entity.receiptReference());
    }

    private static ActionView toView(ClaimActionEntity entity) {
        return new ActionView(
                entity.actionType(),
                entity.actorId(),
                entity.actedAt(),
                entity.resultingState(),
                entity.reason());
    }
}
