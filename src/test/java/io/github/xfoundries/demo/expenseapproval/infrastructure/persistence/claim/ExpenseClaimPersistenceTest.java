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
import org.jfoundry.application.exception.ConflictException;
import io.github.xfoundries.demo.expenseapproval.boot.ExpenseApprovalApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndRestoresCompleteAggregate() {
        ExpenseClaim original = submittedHighValueClaim();

        repository.add(original);
        ExpenseClaim restored = repository.findById(original.id());

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
        repository.add(claim);
        claim.approveByManager(MANAGER, NOW.plusSeconds(3));

        repository.modify(claim);
        ExpenseClaim restored = repository.findById(claim.id());

        assertThat(restored.items()).hasSize(1);
        assertThat(restored.actions()).extracting(action -> action.type())
                .containsExactly(ClaimActionType.SUBMITTED, ClaimActionType.MANAGER_APPROVED);
        assertThat(restored.state()).isEqualTo(ClaimState.PENDING_FINANCE);
    }

    @Test
    void missingAggregateReturnsEmpty() {
        assertThat(repository.findById(ExpenseClaimId.of("missing"))).isNull();
    }

    @Test
    void duplicateAddFailsWithoutImplicitlyModifyingExistingAggregate() {
        ExpenseClaim original = submittedHighValueClaim();
        repository.add(original);

        assertThatThrownBy(() -> repository.add(submittedHighValueClaim()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(original.id().value());

        assertThat(repository.findById(original.id()).actions())
                .extracting(action -> action.type())
                .containsExactly(ClaimActionType.SUBMITTED);
    }

    @Test
    void missingModifyFailsBeforeWritingDependentRecords() {
        ExpenseClaim missing = ExpenseClaim.draft(
                ExpenseClaimId.of("missing-modify"), EMPLOYEE, "Missing", NOW);
        missing.addItem(
                EMPLOYEE,
                new ExpenseItem(
                        ExpenseItemId.of("missing-item"),
                        LocalDate.of(2026, 7, 10),
                        ExpenseCategory.TRAVEL,
                        Money.cny("10.00"),
                        "Taxi",
                        null),
                NOW.plusSeconds(1));

        assertThatThrownBy(() -> repository.modify(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from expense_item where claim_id = ?",
                Integer.class,
                missing.id().value())).isZero();
    }

    @Test
    void staleModifyFailsWithoutReplacingPersistedActions() {
        ExpenseClaim original = submittedHighValueClaim();
        repository.add(original);
        ExpenseClaim firstWriter = repository.findById(original.id());
        ExpenseClaim staleWriter = repository.findById(original.id());

        firstWriter.approveByManager(MANAGER, NOW.plusSeconds(3));
        repository.modify(firstWriter);
        staleWriter.reject(MANAGER, io.github.xfoundries.demo.expenseapproval.domain.model.RejectionReason.of("Stale"), NOW.plusSeconds(4));

        assertThatThrownBy(() -> repository.modify(staleWriter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("optimistic lock conflict");

        assertThat(repository.findById(original.id()).actions())
                .extracting(action -> action.type())
                .containsExactly(ClaimActionType.SUBMITTED, ClaimActionType.MANAGER_APPROVED);
    }

    @Test
    void modifyingAggregateAppendsActionsWithoutDeletingExistingHistory() {
        jdbcTemplate.execute("""
                create table if not exists claim_action_reference (
                    id varchar(80) primary key,
                    action_id varchar(80) not null,
                    constraint fk_claim_action_reference
                        foreign key (action_id) references claim_action(id)
                )
                """);
        ExpenseClaim claim = submittedHighValueClaim();
        repository.add(claim);
        jdbcTemplate.update(
                "insert into claim_action_reference(id, action_id) values (?, ?)",
                "reference-1",
                claim.id().value() + ":0");

        claim.approveByManager(MANAGER, NOW.plusSeconds(3));
        repository.modify(claim);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from claim_action where claim_id = ?",
                Integer.class,
                claim.id().value())).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from claim_action_reference where action_id = ?",
                Integer.class,
                claim.id().value() + ":0")).isOne();
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
