package io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "expense_claim")
public class ExpenseClaimEntity {

    @Id
    private String id;

    @Column(name = "claimant_id", nullable = false)
    private String claimantId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String state;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "finance_approval_required", nullable = false)
    private boolean financeApprovalRequired;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder asc")
    private List<ExpenseItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL)
    @OrderBy("sequenceNo asc")
    private List<ClaimActionEntity> actions = new ArrayList<>();

    protected ExpenseClaimEntity() {
    }

    ExpenseClaimEntity(
            String id,
            String claimantId,
            String title,
            String state,
            BigDecimal totalAmount,
            boolean financeApprovalRequired,
            Instant createdAt,
            Instant updatedAt,
            Instant submittedAt,
            Instant completedAt) {
        this.id = id;
        this.claimantId = claimantId;
        this.title = title;
        this.state = state;
        this.totalAmount = totalAmount;
        this.financeApprovalRequired = financeApprovalRequired;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.submittedAt = submittedAt;
        this.completedAt = completedAt;
    }

    public String id() {
        return id;
    }

    public String claimantId() {
        return claimantId;
    }

    public String title() {
        return title;
    }

    public String state() {
        return state;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public boolean financeApprovalRequired() {
        return financeApprovalRequired;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public List<ExpenseItemEntity> items() {
        return List.copyOf(items);
    }

    public List<ClaimActionEntity> actions() {
        return List.copyOf(actions);
    }

    void apply(
            String title,
            String state,
            BigDecimal totalAmount,
            boolean financeApprovalRequired,
            Instant updatedAt,
            Instant submittedAt,
            Instant completedAt) {
        this.title = title;
        this.state = state;
        this.totalAmount = totalAmount;
        this.financeApprovalRequired = financeApprovalRequired;
        this.updatedAt = updatedAt;
        this.submittedAt = submittedAt;
        this.completedAt = completedAt;
    }

    void replaceItems(List<ExpenseItemEntity> replacement) {
        Map<String, ExpenseItemEntity> existingById = new HashMap<>();
        items.forEach(item -> existingById.put(item.id(), item));
        List<ExpenseItemEntity> desired = new ArrayList<>(replacement.size());
        for (ExpenseItemEntity candidate : replacement) {
            ExpenseItemEntity existing = existingById.remove(candidate.id());
            if (existing == null) {
                addItem(candidate);
                desired.add(candidate);
            } else {
                existing.apply(candidate);
                desired.add(existing);
            }
        }
        items.removeIf(item -> !desired.contains(item));
    }

    void addItem(ExpenseItemEntity item) {
        item.assignTo(this);
        items.add(item);
    }

    void addAction(ClaimActionEntity action) {
        action.assignTo(this);
        actions.add(action);
    }
}
