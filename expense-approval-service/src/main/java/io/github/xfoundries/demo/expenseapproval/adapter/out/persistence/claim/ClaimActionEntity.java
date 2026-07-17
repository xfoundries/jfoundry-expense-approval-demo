package io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "claim_action")
public class ClaimActionEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private ExpenseClaimEntity claim;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "acted_at", nullable = false)
    private Instant actedAt;

    @Column(name = "resulting_state", nullable = false)
    private String resultingState;

    private String reason;

    protected ClaimActionEntity() {
    }

    ClaimActionEntity(
            String id,
            int sequenceNo,
            String actionType,
            String actorId,
            Instant actedAt,
            String resultingState,
            String reason) {
        this.id = id;
        this.sequenceNo = sequenceNo;
        this.actionType = actionType;
        this.actorId = actorId;
        this.actedAt = actedAt;
        this.resultingState = resultingState;
        this.reason = reason;
    }

    public int sequenceNo() {
        return sequenceNo;
    }

    public String actionType() {
        return actionType;
    }

    public String actorId() {
        return actorId;
    }

    public Instant actedAt() {
        return actedAt;
    }

    public String resultingState() {
        return resultingState;
    }

    public String reason() {
        return reason;
    }

    void assignTo(ExpenseClaimEntity claim) {
        this.claim = claim;
    }
}
