package io.github.xfoundries.demo.expenseapproval.adapter.out.persistence.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_item")
public class ExpenseItemEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private ExpenseClaimEntity claim;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Column(name = "receipt_reference")
    private String receiptReference;

    protected ExpenseItemEntity() {
    }

    ExpenseItemEntity(
            String id,
            int itemOrder,
            LocalDate expenseDate,
            String category,
            BigDecimal amount,
            String description,
            String receiptReference) {
        this.id = id;
        this.itemOrder = itemOrder;
        this.expenseDate = expenseDate;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.receiptReference = receiptReference;
    }

    public String id() {
        return id;
    }

    public int itemOrder() {
        return itemOrder;
    }

    public LocalDate expenseDate() {
        return expenseDate;
    }

    public String category() {
        return category;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String description() {
        return description;
    }

    public String receiptReference() {
        return receiptReference;
    }

    void apply(ExpenseItemEntity source) {
        itemOrder = source.itemOrder;
        expenseDate = source.expenseDate;
        category = source.category;
        amount = source.amount;
        description = source.description;
        receiptReference = source.receiptReference;
    }

    void assignTo(ExpenseClaimEntity claim) {
        this.claim = claim;
    }
}
