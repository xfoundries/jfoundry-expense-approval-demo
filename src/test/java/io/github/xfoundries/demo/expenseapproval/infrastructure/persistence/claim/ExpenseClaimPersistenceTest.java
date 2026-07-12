package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimActionType;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseCategory;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import io.github.xfoundries.demo.expenseapproval.boot.ExpenseApprovalApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExpenseApprovalApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:expense-persistence;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always"
})
@Transactional
class ExpenseClaimPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");
    private static final UserId EMPLOYEE = UserId.of("employee-1");
    private static final UserId MANAGER = UserId.of("manager-1");

    @Autowired
    private ExpenseClaimRepository repository;

    @Test
    void savesAndRestoresCompleteAggregate() {
        ExpenseClaim original = submittedHighValueClaim();

        repository.save(original);
        ExpenseClaim restored = repository.findById(original.id()).orElseThrow();

        assertThat(restored.id()).isEqualTo(original.id());
        assertThat(restored.claimant()).isEqualTo(EMPLOYEE);
        assertThat(restored.state()).isEqualTo(ClaimState.PENDING_MANAGER);
        assertThat(restored.total()).isEqualTo(Money.cny("2000.01"));
        assertThat(restored.items()).hasSize(1);
        assertThat(restored.items().getFirst().description()).isEqualTo("Hotel");
        assertThat(restored.actions()).extracting(action -> action.type())
                .containsExactly(ClaimActionType.SUBMITTED);
        assertThat(restored.financeApprovalRequired()).isTrue();
        assertThat(restored.submittedAt()).contains(NOW.plusSeconds(2));
    }

    @Test
    void updatingAggregateReplacesItemsAndAppendsActions() {
        ExpenseClaim claim = submittedHighValueClaim();
        repository.save(claim);
        claim.approveByManager(MANAGER, NOW.plusSeconds(3));

        repository.save(claim);
        ExpenseClaim restored = repository.findById(claim.id()).orElseThrow();

        assertThat(restored.items()).hasSize(1);
        assertThat(restored.actions()).extracting(action -> action.type())
                .containsExactly(ClaimActionType.SUBMITTED, ClaimActionType.MANAGER_APPROVED);
        assertThat(restored.state()).isEqualTo(ClaimState.PENDING_FINANCE);
    }

    @Test
    void missingAggregateReturnsEmpty() {
        assertThat(repository.findById(ExpenseClaimId.of("missing"))).isEmpty();
    }

    private static ExpenseClaim submittedHighValueClaim() {
        ExpenseClaim claim = ExpenseClaim.draft(
                ExpenseClaimId.of("claim-persistence"), EMPLOYEE, "Customer visit", NOW);
        claim.addItem(
                EMPLOYEE,
                new ExpenseItem(
                        ExpenseItemId.of("item-persistence"),
                        LocalDate.of(2026, 7, 10),
                        ExpenseCategory.LODGING,
                        Money.positiveCny(new BigDecimal("2000.01")),
                        "Hotel",
                        "receipt-1"),
                NOW.plusSeconds(1));
        claim.submit(EMPLOYEE, NOW.plusSeconds(2));
        return claim;
    }
}
