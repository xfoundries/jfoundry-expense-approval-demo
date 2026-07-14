package io.github.xfoundries.demo.expenseapproval.application.approval;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import io.github.xfoundries.demo.contracts.EventEnvelope;
import io.github.xfoundries.demo.contracts.ExpenseClaimApprovedV1;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByFinanceCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimApproved;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import io.github.xfoundries.demo.expenseapproval.domain.policy.MonthlyExpenseLimitPolicy;
import org.jfoundry.application.exception.ExternalAccessException;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;
import org.jfoundry.application.lock.LockOptions;
import org.jfoundry.application.lock.LockTemplate;
import org.jfoundry.application.outbox.OutboxAppendRequest;
import org.jfoundry.application.outbox.OutboxTemplate;
import org.jfoundry.application.transaction.TransactionCallback;
import org.jfoundry.application.transaction.TransactionRunner;
import org.jmolecules.event.types.DomainEvent;

public final class FinalApprovalCoordinator {

    private static final String OUTBOX_TOPIC = "expense-approval.events.v1";

    private final ExpenseClaimRepository repository;
    private final ApprovedExpenseAmountReader approvedExpenseAmountReader;
    private final MonthlyExpenseLimitPolicy limitPolicy;
    private final LockTemplate lockTemplate;
    private final TransactionRunner transactionRunner;
    private final OutboxTemplate outboxTemplate;
    private final ExpenseClaimApprovedTranslator translator;
    private final Clock clock;
    private final ZoneId businessZone;
    private final LockOptions lockOptions;

    public FinalApprovalCoordinator(
            ExpenseClaimRepository repository,
            ApprovedExpenseAmountReader approvedExpenseAmountReader,
            MonthlyExpenseLimitPolicy limitPolicy,
            LockTemplate lockTemplate,
            TransactionRunner transactionRunner,
            OutboxTemplate outboxTemplate,
            ExpenseClaimApprovedTranslator translator,
            Clock clock,
            ZoneId businessZone,
            Duration lockWaitTime) {
        this.repository = Objects.requireNonNull(repository);
        this.approvedExpenseAmountReader = Objects.requireNonNull(approvedExpenseAmountReader);
        this.limitPolicy = Objects.requireNonNull(limitPolicy);
        this.lockTemplate = Objects.requireNonNull(lockTemplate);
        this.transactionRunner = Objects.requireNonNull(transactionRunner);
        this.outboxTemplate = Objects.requireNonNull(outboxTemplate);
        this.translator = Objects.requireNonNull(translator);
        this.clock = Objects.requireNonNull(clock);
        this.businessZone = Objects.requireNonNull(businessZone);
        this.lockOptions = LockOptions.builder()
                .waitTime(Objects.requireNonNull(lockWaitTime))
                .build();
    }

    public void approveByManager(ApproveExpenseClaimByManagerCommand command) {
        requireRole(command.actor(), ApprovalRole.MANAGER);
        ApprovalSnapshot snapshot = inTransaction(() -> {
            ExpenseClaim claim = load(command.claimId());
            claim.validateManagerApproval(command.actor().userId());
            return new ApprovalSnapshot(claim.claimant(), claim.financeApprovalRequired());
        });
        if (snapshot.financeApprovalRequired()) {
            inTransaction(() -> {
                ExpenseClaim claim = load(command.claimId());
                claim.approveByManager(command.actor().userId(), clock.instant());
                repository.modify(claim);
                return null;
            });
            return;
        }
        approveFinally(command.claimId(), command.actor(), snapshot.claimant(), ApprovalStage.MANAGER);
    }

    public void approveByFinance(ApproveExpenseClaimByFinanceCommand command) {
        requireRole(command.actor(), ApprovalRole.FINANCE);
        UserId claimant = inTransaction(() -> {
            ExpenseClaim claim = load(command.claimId());
            claim.validateFinanceApproval(command.actor().userId());
            return claim.claimant();
        });
        approveFinally(command.claimId(), command.actor(), claimant, ApprovalStage.FINANCE);
    }

    private void approveFinally(
            ExpenseClaimId claimId, ApprovalActor actor, UserId claimant, ApprovalStage stage) {
        Instant approvedAt = clock.instant();
        YearMonth month = YearMonth.from(approvedAt.atZone(businessZone));
        String lockName = "expense-approval:monthly-limit:%s:%s"
                .formatted(claimant.value(), month);
        try {
            lockTemplate.execute(lockName, lockOptions, () -> {
                inTransaction(() -> {
                    completeFinalApproval(claimId, actor, claimant, stage, approvedAt, month);
                    return null;
                });
                return null;
            });
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new ExternalAccessException("Unable to coordinate final expense approval", failure);
        }
    }

    private void completeFinalApproval(
            ExpenseClaimId claimId,
            ApprovalActor actor,
            UserId expectedClaimant,
            ApprovalStage stage,
            Instant approvedAt,
            YearMonth month) {
        ExpenseClaim claim = load(claimId);
        if (!claim.claimant().equals(expectedClaimant)) {
            throw new IllegalStateException("Expense claim claimant changed unexpectedly");
        }
        if (stage == ApprovalStage.MANAGER) {
            claim.validateManagerApproval(actor.userId());
        } else {
            claim.validateFinanceApproval(actor.userId());
        }
        Instant from = month.atDay(1).atStartOfDay(businessZone).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(businessZone).toInstant();
        limitPolicy.ensureWithinLimit(
                approvedExpenseAmountReader.totalApproved(expectedClaimant, from, to), claim.total());
        if (stage == ApprovalStage.MANAGER) {
            claim.approveByManager(actor.userId(), approvedAt);
        } else {
            claim.approveByFinance(actor.userId(), approvedAt);
        }
        repository.modify(claim);
        appendApprovalEvent(claim);
    }

    private void appendApprovalEvent(ExpenseClaim claim) {
        List<DomainEvent> events = claim.drainEvents();
        if (events.size() != 1 || !(events.getFirst() instanceof ExpenseClaimApproved approved)) {
            throw new IllegalStateException("Final approval must record one ExpenseClaimApproved event");
        }
        EventEnvelope<ExpenseClaimApprovedV1> envelope = translator.translate(approved);
        outboxTemplate.append(new OutboxAppendRequest(
                envelope.eventId(),
                OUTBOX_TOPIC,
                claim.id().value(),
                "ExpenseClaimApprovedV1",
                envelope,
                envelope.occurredAt(),
                "ExpenseClaim",
                claim.id().value(),
                null));
    }

    private ExpenseClaim load(ExpenseClaimId id) {
        ExpenseClaim claim = repository.findById(id);
        if (claim == null) {
            throw new NotFoundException("Expense claim not found: " + id.value());
        }
        return claim;
    }

    private <T> T inTransaction(TransactionCallback<T> callback) {
        try {
            return transactionRunner.call(callback);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new ExternalAccessException("Expense approval transaction failed", failure);
        }
    }

    private static void requireRole(ApprovalActor actor, ApprovalRole role) {
        if (actor.role() != role) {
            throw new InvalidArgumentException("This action requires role " + role);
        }
    }

    private enum ApprovalStage {
        MANAGER,
        FINANCE
    }

    private record ApprovalSnapshot(UserId claimant, boolean financeApprovalRequired) {
    }
}
