package io.github.xfoundries.demo.expenseapproval.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jfoundry.domain.entity.agg.BaseAggregateRoot;
import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jfoundry.domain.exception.DomainStateException;

public final class ExpenseClaim extends BaseAggregateRoot<ExpenseClaim, ExpenseClaimId> {

    private static final Money APPROVAL_THRESHOLD = Money.cny("2000.00");

    private final UserId claimant;
    private String title;
    private ClaimState state;
    private final Map<ExpenseItemId, ExpenseItem> items;
    private final List<ClaimAction> actions;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant submittedAt;
    private Instant completedAt;
    private boolean financeApprovalRequired;

    private ExpenseClaim(
            ExpenseClaimId id,
            UserId claimant,
            String title,
            ClaimState state,
            Collection<ExpenseItem> items,
            Collection<ClaimAction> actions,
            Instant createdAt,
            Instant updatedAt,
            Instant submittedAt,
            Instant completedAt,
            boolean financeApprovalRequired) {
        super(require(id, "Expense claim id is required"));
        this.claimant = require(claimant, "Claimant is required");
        this.title = validateTitle(title);
        this.state = require(state, "Claim state is required");
        this.items = new LinkedHashMap<>();
        items.forEach(item -> this.items.put(item.id(), item));
        this.actions = new ArrayList<>(actions);
        this.createdAt = require(createdAt, "Created time is required");
        this.updatedAt = require(updatedAt, "Updated time is required");
        this.submittedAt = submittedAt;
        this.completedAt = completedAt;
        this.financeApprovalRequired = financeApprovalRequired;
    }

    public static ExpenseClaim draft(
            ExpenseClaimId id, UserId claimant, String title, Instant now) {
        return new ExpenseClaim(
                id, claimant, title, ClaimState.DRAFT, List.of(), List.of(),
                now, now, null, null, false);
    }

    public static ExpenseClaim restore(
            ExpenseClaimId id,
            UserId claimant,
            String title,
            ClaimState state,
            Collection<ExpenseItem> items,
            Collection<ClaimAction> actions,
            Instant createdAt,
            Instant updatedAt,
            Instant submittedAt,
            Instant completedAt,
            boolean financeApprovalRequired) {
        return new ExpenseClaim(
                id, claimant, title, state, items, actions, createdAt, updatedAt,
                submittedAt, completedAt, financeApprovalRequired);
    }

    public void addItem(UserId actor, ExpenseItem item, Instant now) {
        requireEditableBy(actor);
        require(item, "Expense item is required");
        if (items.containsKey(item.id())) {
            throw new DomainRuleViolationException("Expense item already exists: " + item.id().value());
        }
        items.put(item.id(), item);
        updatedAt = now;
    }

    public void updateItem(UserId actor, ExpenseItem item, Instant now) {
        requireEditableBy(actor);
        require(item, "Expense item is required");
        if (!items.containsKey(item.id())) {
            throw new DomainRuleViolationException("Expense item not found: " + item.id().value());
        }
        items.put(item.id(), item);
        updatedAt = now;
    }

    public void removeItem(UserId actor, ExpenseItemId itemId, Instant now) {
        requireEditableBy(actor);
        if (items.remove(itemId) == null) {
            throw new DomainRuleViolationException("Expense item not found: " + itemId.value());
        }
        updatedAt = now;
    }

    public void submit(UserId actor, Instant now) {
        requireState(ClaimState.DRAFT);
        requireClaimant(actor);
        if (items.isEmpty()) {
            throw new DomainRuleViolationException("Expense claim must contain at least one item");
        }
        financeApprovalRequired = total().isGreaterThan(APPROVAL_THRESHOLD);
        state = ClaimState.PENDING_MANAGER;
        submittedAt = now;
        completedAt = null;
        updatedAt = now;
        record(ClaimActionType.SUBMITTED, actor, null, now);
    }

    public void approveByManager(UserId actor, Instant now) {
        validateManagerApproval(actor);
        state = financeApprovalRequired ? ClaimState.PENDING_FINANCE : ClaimState.APPROVED;
        if (state == ClaimState.APPROVED) {
            completedAt = now;
            recordEvent(new ExpenseClaimApproved(id(), claimant, total(), now));
        }
        updatedAt = now;
        record(ClaimActionType.MANAGER_APPROVED, actor, null, now);
    }

    public void approveByFinance(UserId actor, Instant now) {
        validateFinanceApproval(actor);
        state = ClaimState.APPROVED;
        completedAt = now;
        updatedAt = now;
        record(ClaimActionType.FINANCE_APPROVED, actor, null, now);
        recordEvent(new ExpenseClaimApproved(id(), claimant, total(), now));
    }

    public void reject(UserId actor, RejectionReason reason, Instant now) {
        if (state != ClaimState.PENDING_MANAGER && state != ClaimState.PENDING_FINANCE) {
            throw new DomainStateException("Expense claim is not waiting for approval");
        }
        rejectSelfApproval(actor);
        state = ClaimState.REJECTED;
        completedAt = now;
        updatedAt = now;
        record(ClaimActionType.REJECTED, actor, reason.value(), now);
    }

    public void reopen(UserId actor, Instant now) {
        requireState(ClaimState.REJECTED);
        requireClaimant(actor);
        state = ClaimState.DRAFT;
        submittedAt = null;
        completedAt = null;
        financeApprovalRequired = false;
        updatedAt = now;
        record(ClaimActionType.REOPENED, actor, null, now);
    }

    public void withdraw(UserId actor, Instant now) {
        if (state != ClaimState.PENDING_MANAGER && state != ClaimState.PENDING_FINANCE) {
            throw new DomainStateException("Only a submitted expense claim can be withdrawn");
        }
        requireClaimant(actor);
        state = ClaimState.WITHDRAWN;
        completedAt = now;
        updatedAt = now;
        record(ClaimActionType.WITHDRAWN, actor, null, now);
    }

    public ExpenseClaimId id() {
        return getId();
    }

    public UserId claimant() {
        return claimant;
    }

    public String title() {
        return title;
    }

    public ClaimState state() {
        return state;
    }

    public List<ExpenseItem> items() {
        return List.copyOf(items.values());
    }

    public List<ClaimAction> actions() {
        return List.copyOf(actions);
    }

    public Money total() {
        return items.values().stream()
                .map(ExpenseItem::amount)
                .reduce(Money.zeroCny(), Money::add);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Optional<Instant> submittedAt() {
        return Optional.ofNullable(submittedAt);
    }

    public Optional<Instant> completedAt() {
        return Optional.ofNullable(completedAt);
    }

    public boolean financeApprovalRequired() {
        return financeApprovalRequired;
    }

    public void validateManagerApproval(UserId actor) {
        requireState(ClaimState.PENDING_MANAGER);
        rejectSelfApproval(actor);
    }

    public void validateFinanceApproval(UserId actor) {
        requireState(ClaimState.PENDING_FINANCE);
        rejectSelfApproval(actor);
    }

    private void requireEditableBy(UserId actor) {
        requireState(ClaimState.DRAFT);
        requireClaimant(actor);
    }

    private void requireClaimant(UserId actor) {
        if (!claimant.equals(actor)) {
            throw new DomainRuleViolationException("Only the claimant can perform this action");
        }
    }

    private void rejectSelfApproval(UserId actor) {
        if (claimant.equals(actor)) {
            throw new DomainRuleViolationException("A claimant cannot approve or reject their own claim");
        }
    }

    private void requireState(ClaimState required) {
        if (state != required) {
            throw new DomainStateException(
                    "Expense claim must be " + required + " but is " + state);
        }
    }

    private void record(ClaimActionType type, UserId actor, String reason, Instant now) {
        actions.add(ClaimAction.of(type, actor, now, state, reason));
    }

    private static String validateTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainRuleViolationException("Expense claim title is required");
        }
        value = value.trim();
        if (value.length() > 200) {
            throw new DomainRuleViolationException("Expense claim title must not exceed 200 characters");
        }
        return value;
    }

    private static <T> T require(T value, String message) {
        if (value == null) {
            throw new DomainRuleViolationException(message);
        }
        return value;
    }
}
