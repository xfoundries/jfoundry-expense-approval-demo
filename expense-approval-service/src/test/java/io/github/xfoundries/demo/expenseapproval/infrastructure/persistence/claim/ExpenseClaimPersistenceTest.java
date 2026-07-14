package io.github.xfoundries.demo.expenseapproval.infrastructure.persistence.claim;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import io.github.xfoundries.demo.expenseapproval.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = ExpenseApprovalApplication.class)
@Transactional
class ExpenseClaimPersistenceTest extends PostgreSqlIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");
    private static final UserId EMPLOYEE = UserId.of("employee-1");
    private static final UserId MANAGER = UserId.of("manager-1");

    @Autowired
    private ExpenseClaimRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void savesAndRestoresCompleteAggregate() {
        ExpenseClaim original = submittedHighValueClaim("claim-save");

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
        ExpenseClaim claim = submittedHighValueClaim("claim-update");
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void duplicateAddFailsWithoutImplicitlyModifyingExistingAggregate() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ExpenseClaim original = submittedHighValueClaim("claim-duplicate");
        transaction.executeWithoutResult(status -> repository.add(original));

        assertThatThrownBy(() -> transaction.executeWithoutResult(
                status -> repository.add(submittedHighValueClaim("claim-duplicate"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(original.id().value());

        transaction.executeWithoutResult(status -> assertThat(repository.findById(original.id()).actions())
                .extracting(action -> action.type())
                .containsExactly(ClaimActionType.SUBMITTED));
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
                .hasMessageContaining("not tracked");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from expense_item where claim_id = ?",
                Integer.class,
                missing.id().value())).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentTransactionsCannotBothModifyTheSameLoadedVersion() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ExpenseClaim original = submittedHighValueClaim("claim-concurrent");
        transaction.executeWithoutResult(status -> repository.add(original));

        CountDownLatch staleLoaded = new CountDownLatch(1);
        CountDownLatch winnerCommitted = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Throwable> staleResult = executor.submit(() -> {
                try {
                    transaction.executeWithoutResult(status -> {
                        ExpenseClaim stale = repository.findById(original.id());
                        staleLoaded.countDown();
                        await(winnerCommitted);
                        stale.reject(
                                MANAGER,
                                io.github.xfoundries.demo.expenseapproval.domain.model.RejectionReason.of("Stale"),
                                NOW.plusSeconds(4));
                        repository.modify(stale);
                    });
                    return null;
                } catch (Throwable failure) {
                    return failure;
                }
            });

            assertThat(staleLoaded.await(5, TimeUnit.SECONDS)).isTrue();
            transaction.executeWithoutResult(status -> {
                ExpenseClaim winner = repository.findById(original.id());
                winner.approveByManager(MANAGER, NOW.plusSeconds(3));
                repository.modify(winner);
            });
            winnerCommitted.countDown();

            assertThat(staleResult.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("optimistic lock conflict");
        }

        transaction.executeWithoutResult(status -> assertThat(repository.findById(original.id()).actions())
                .extracting(action -> action.type())
                .containsExactly(ClaimActionType.SUBMITTED, ClaimActionType.MANAGER_APPROVED));
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
        ExpenseClaim claim = submittedHighValueClaim("claim-history");
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

    private static ExpenseClaim submittedHighValueClaim(String claimId) {
        ExpenseClaim claim = ExpenseClaim.draft(
                ExpenseClaimId.of(claimId), EMPLOYEE, "Customer visit", NOW);
        claim.addItem(
                EMPLOYEE,
                new ExpenseItem(
                        ExpenseItemId.of("item-" + claimId),
                        LocalDate.of(2026, 7, 10),
                        ExpenseCategory.LODGING,
                        Money.positiveCny(new BigDecimal("2000.01")),
                        "Hotel",
                        "receipt-1"),
                NOW.plusSeconds(1));
        claim.submit(EMPLOYEE, NOW.plusSeconds(2));
        return claim;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent transaction");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent transaction", exception);
        }
    }
}
