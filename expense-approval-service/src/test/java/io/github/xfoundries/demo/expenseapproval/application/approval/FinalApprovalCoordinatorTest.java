package io.github.xfoundries.demo.expenseapproval.application.approval;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import io.github.xfoundries.demo.expenseapproval.application.approval.port.out.ApprovedExpenseAmountPort;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByFinanceCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseCategory;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.domain.policy.MonthlyExpenseLimitExceeded;
import io.github.xfoundries.demo.expenseapproval.domain.policy.MonthlyExpenseLimitPolicy;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import org.jfoundry.application.lock.LockHandle;
import org.jfoundry.application.lock.LockTemplate;
import org.jfoundry.application.outbox.OutboxAppendRequest;
import org.jfoundry.application.outbox.OutboxTemplate;
import org.jfoundry.application.transaction.TransactionCallback;
import org.jfoundry.application.transaction.TransactionOptions;
import org.jfoundry.application.transaction.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalApprovalCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-07-31T16:30:00Z");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final UserId EMPLOYEE = UserId.of("employee-1");
    private static final ApprovalActor MANAGER = new ApprovalActor(UserId.of("manager-1"), ApprovalRole.MANAGER);

    @Mock ExpenseClaimRepository repository;
    @Mock ApprovedExpenseAmountPort approvedExpenseAmountPort;
    @Mock OutboxTemplate outboxTemplate;

    private List<String> boundaries;
    private RecordingTransactionRunner transactions;
    private FinalApprovalCoordinator coordinator;

    @BeforeEach
    void setUp() {
        boundaries = new ArrayList<>();
        transactions = new RecordingTransactionRunner(boundaries);
        LockTemplate locks = new LockTemplate((name, options) -> {
            boundaries.add("lock:" + name);
            return new LockHandle(name, true, () -> boundaries.add("unlock:" + name));
        });
        coordinator = new FinalApprovalCoordinator(
                repository,
                approvedExpenseAmountPort,
                new MonthlyExpenseLimitPolicy(Money.cny("10000.00")),
                locks,
                transactions,
                outboxTemplate,
                new ExpenseClaimApprovedTranslator(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                BUSINESS_ZONE,
                Duration.ofSeconds(2));
    }

    @Test
    void finalManagerApprovalChecksLimitAndAppendsOutboxInsideTheLock() {
        ExpenseClaim claim = submittedClaim("claim-low", "2000.00");
        when(repository.findById(claim.id())).thenReturn(claim);
        when(approvedExpenseAmountPort.totalApproved(any(), any(), any()))
                .thenReturn(Money.cny("7000.00"));

        coordinator.approveByManager(
                new ApproveExpenseClaimByManagerCommand(MANAGER, claim.id()));

        assertThat(claim.state()).isEqualTo(ClaimState.APPROVED);
        assertThat(boundaries).containsExactly(
                "transaction",
                "lock:expense-approval:monthly-limit:employee-1:2026-08",
                "transaction",
                "unlock:expense-approval:monthly-limit:employee-1:2026-08");
        verify(repository).modify(claim);
        ArgumentCaptor<OutboxAppendRequest> request =
                ArgumentCaptor.forClass(OutboxAppendRequest.class);
        verify(outboxTemplate).append(request.capture());
        assertThat(request.getValue().topic()).isEqualTo("expense-approval.events.v1");
        assertThat(request.getValue().payloadKey()).isEqualTo("claim-low");
        assertThat(request.getValue().payloadType()).isEqualTo("ExpenseClaimApprovedV1");
    }

    @Test
    void limitFailureLeavesClaimPendingAndWritesNothing() {
        ExpenseClaim claim = submittedClaim("claim-over-limit", "2000.01");
        claim.approveByManager(MANAGER.userId(), NOW.minusSeconds(1));
        ApprovalActor finance = new ApprovalActor(UserId.of("finance-1"), ApprovalRole.FINANCE);
        when(repository.findById(claim.id())).thenReturn(claim);
        when(approvedExpenseAmountPort.totalApproved(any(), any(), any()))
                .thenReturn(Money.cny("8000.00"));

        assertThatThrownBy(() -> coordinator.approveByFinance(
                new ApproveExpenseClaimByFinanceCommand(finance, claim.id())))
                .isInstanceOf(MonthlyExpenseLimitExceeded.class);

        assertThat(claim.state()).isEqualTo(ClaimState.PENDING_FINANCE);
        verify(repository, never()).modify(claim);
        verify(outboxTemplate, never()).append(any());
    }

    @Test
    void highValueManagerApprovalOnlyRoutesToFinance() {
        ExpenseClaim claim = submittedClaim("claim-high", "2000.01");
        when(repository.findById(claim.id())).thenReturn(claim);

        coordinator.approveByManager(
                new ApproveExpenseClaimByManagerCommand(MANAGER, claim.id()));

        assertThat(claim.state()).isEqualTo(ClaimState.PENDING_FINANCE);
        assertThat(boundaries).containsExactly("transaction", "transaction");
        verify(repository).modify(claim);
        verify(approvedExpenseAmountPort, never()).totalApproved(any(), any(), any());
        verify(outboxTemplate, never()).append(any());
    }

    private static ExpenseClaim submittedClaim(String id, String amount) {
        ExpenseClaim claim = ExpenseClaim.draft(
                ExpenseClaimId.of(id), EMPLOYEE, "Customer visit", NOW.minusSeconds(3));
        claim.addItem(EMPLOYEE, new ExpenseItem(
                ExpenseItemId.of("item-" + id), LocalDate.of(2026, 7, 10),
                ExpenseCategory.TRAVEL, Money.positiveCny(new BigDecimal(amount)),
                "Taxi", "receipt-1"), NOW.minusSeconds(2));
        claim.submit(EMPLOYEE, NOW.minusSeconds(1));
        return claim;
    }

    private static final class RecordingTransactionRunner implements TransactionRunner {

        private final List<String> boundaries;

        private RecordingTransactionRunner(List<String> boundaries) {
            this.boundaries = boundaries;
        }

        @Override
        public <T> T call(TransactionOptions options, TransactionCallback<T> callback)
                throws Exception {
            boundaries.add("transaction");
            return callback.execute();
        }
    }
}
