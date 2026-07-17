package io.github.xfoundries.demo.expenseapproval.adapter.out.projection.payment;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "claim_payment_status")
public class PaymentStatusEntity {

    @Id
    @Column(name = "claim_id")
    private String claimId;

    @Column(nullable = false)
    private String status;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "source_event_id", nullable = false, unique = true)
    private String sourceEventId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentStatusEntity() {
    }

    public PaymentStatusEntity(
            String claimId,
            String status,
            String paymentReference,
            String failureCode,
            String failureReason,
            Instant processedAt,
            String sourceEventId) {
        this.claimId = claimId;
        apply(
                status,
                paymentReference,
                failureCode,
                failureReason,
                processedAt,
                sourceEventId);
    }

    public String claimId() {
        return claimId;
    }

    public String status() {
        return status;
    }

    public String paymentReference() {
        return paymentReference;
    }

    public String failureCode() {
        return failureCode;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant processedAt() {
        return processedAt;
    }

    public void apply(
            String status,
            String paymentReference,
            String failureCode,
            String failureReason,
            Instant processedAt,
            String sourceEventId) {
        this.status = status;
        this.paymentReference = paymentReference;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.processedAt = processedAt;
        this.sourceEventId = sourceEventId;
        this.updatedAt = processedAt;
    }
}
