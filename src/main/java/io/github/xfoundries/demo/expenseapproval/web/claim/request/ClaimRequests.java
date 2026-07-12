package io.github.xfoundries.demo.expenseapproval.web.claim.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ClaimRequests {

    private ClaimRequests() {
    }

    public record CreateClaimRequest(
            @NotBlank @Size(max = 200) String title) {
    }

    public record ExpenseItemRequest(
            @NotNull LocalDate expenseDate,
            @NotNull ExpenseCategory category,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
            @NotBlank @Size(max = 500) String description,
            @Size(max = 500) String receiptReference) {
    }

    public record RejectClaimRequest(
            @NotBlank @Size(max = 500) String reason) {
    }
}

