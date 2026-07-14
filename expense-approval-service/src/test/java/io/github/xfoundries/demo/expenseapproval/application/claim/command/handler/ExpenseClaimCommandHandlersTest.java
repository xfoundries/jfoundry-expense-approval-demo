package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.AddExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByFinanceCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.approval.FinalApprovalCoordinator;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RejectExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RemoveExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ReopenExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.SubmitExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.UpdateExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.WithdrawExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseCategory;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseClaimCommandHandlersTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UserId EMPLOYEE_ID = UserId.of("employee-1");
    private static final ApprovalActor EMPLOYEE = new ApprovalActor(EMPLOYEE_ID, ApprovalRole.EMPLOYEE);
    private static final ApprovalActor MANAGER = new ApprovalActor(UserId.of("manager-1"), ApprovalRole.MANAGER);
    private static final ApprovalActor FINANCE = new ApprovalActor(UserId.of("finance-1"), ApprovalRole.FINANCE);

    @Mock
    private ExpenseClaimRepository repository;

    @Mock
    private FinalApprovalCoordinator finalApprovalCoordinator;

    private ClaimCommandContext context;

    @BeforeEach
    void setUp() {
        context = new ClaimCommandContext(repository, CLOCK);
    }

    @Test
    void itemHandlersLoadMutateAndSaveTheAggregate() {
        ExpenseClaim claim = ExpenseClaim.draft(
                ExpenseClaimId.of("claim-items"), EMPLOYEE_ID, "Visit", NOW.minusSeconds(1));
        when(repository.findById(claim.id())).thenReturn(claim);

        ExpenseItemId itemId = new AddExpenseItemCommandHandler(context).handle(
                new AddExpenseItemCommand(EMPLOYEE, claim.id(), LocalDate.of(2026, 7, 10),
                        ExpenseCategory.TRAVEL, new BigDecimal("10.00"), "Taxi", null));
        new UpdateExpenseItemCommandHandler(context).handle(
                new UpdateExpenseItemCommand(EMPLOYEE, claim.id(), itemId,
                        LocalDate.of(2026, 7, 11), ExpenseCategory.MEAL,
                        new BigDecimal("20.00"), "Dinner", "receipt-1"));
        new RemoveExpenseItemCommandHandler(context).handle(
                new RemoveExpenseItemCommand(EMPLOYEE, claim.id(), itemId));

        assertThat(claim.items()).isEmpty();
        verify(repository, times(3)).modify(claim);
    }

    @Test
    void employeeLifecycleHandlersPreserveStateTransitions() {
        ExpenseClaim withdrawn = draftWithItem("claim-withdraw", "100.00");
        ExpenseClaim rejected = submittedClaim("claim-reopen", "100.00");
        rejected.reject(MANAGER.userId(),
                io.github.xfoundries.demo.expenseapproval.domain.model.RejectionReason.of("Fix it"),
                NOW.minusMillis(500));
        when(repository.findById(withdrawn.id())).thenReturn(withdrawn);
        when(repository.findById(rejected.id())).thenReturn(rejected);

        new SubmitExpenseClaimCommandHandler(context).handle(
                new SubmitExpenseClaimCommand(EMPLOYEE, withdrawn.id()));
        new WithdrawExpenseClaimCommandHandler(context).handle(
                new WithdrawExpenseClaimCommand(EMPLOYEE, withdrawn.id()));
        new ReopenExpenseClaimCommandHandler(context).handle(
                new ReopenExpenseClaimCommand(EMPLOYEE, rejected.id()));

        assertThat(withdrawn.state()).isEqualTo(ClaimState.WITHDRAWN);
        assertThat(rejected.state()).isEqualTo(ClaimState.DRAFT);
        verify(repository, times(2)).modify(withdrawn);
        verify(repository).modify(rejected);
    }

    @Test
    void approvalHandlersDelegateToTheCoordinatorWithoutOwningTheTransaction() {
        ExpenseClaimId id = ExpenseClaimId.of("claim-1");
        ApproveExpenseClaimByManagerCommand manager =
                new ApproveExpenseClaimByManagerCommand(MANAGER, id);
        ApproveExpenseClaimByFinanceCommand finance =
                new ApproveExpenseClaimByFinanceCommand(FINANCE, id);

        new ApproveExpenseClaimByManagerCommandHandler(finalApprovalCoordinator).handle(manager);
        new ApproveExpenseClaimByFinanceCommandHandler(finalApprovalCoordinator).handle(finance);

        verify(finalApprovalCoordinator).approveByManager(manager);
        verify(finalApprovalCoordinator).approveByFinance(finance);
    }

    @Test
    void rejectionRoleMustMatchCurrentApprovalStage() {
        ExpenseClaim claim = submittedClaim("claim-reject", "2000.01");
        when(repository.findById(claim.id())).thenReturn(claim);
        RejectExpenseClaimCommandHandler handler = new RejectExpenseClaimCommandHandler(context);

        assertThatThrownBy(() -> handler.handle(
                new RejectExpenseClaimCommand(FINANCE, claim.id(), "Missing receipt")))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("MANAGER");

        handler.handle(new RejectExpenseClaimCommand(MANAGER, claim.id(), "Missing receipt"));
        assertThat(claim.state()).isEqualTo(ClaimState.REJECTED);
        verify(repository).modify(claim);
    }

    private static ExpenseClaim submittedClaim(String claimId, String amount) {
        ExpenseClaim claim = draftWithItem(claimId, amount);
        claim.submit(EMPLOYEE_ID, NOW.minusSeconds(1));
        return claim;
    }

    private static ExpenseClaim draftWithItem(String claimId, String amount) {
        ExpenseClaim claim = ExpenseClaim.draft(
                ExpenseClaimId.of(claimId), EMPLOYEE_ID, "Customer visit", NOW.minusSeconds(3));
        claim.addItem(
                EMPLOYEE_ID,
                new ExpenseItem(
                        ExpenseItemId.of("item-" + claimId),
                        LocalDate.of(2026, 7, 10),
                        ExpenseCategory.TRAVEL,
                        Money.positiveCny(new BigDecimal(amount)),
                        "Taxi",
                        "receipt-1"),
                NOW.minusSeconds(2));
        return claim;
    }
}
