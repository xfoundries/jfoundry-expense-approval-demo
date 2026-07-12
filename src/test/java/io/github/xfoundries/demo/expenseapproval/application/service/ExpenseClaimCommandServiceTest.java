package io.github.xfoundries.demo.expenseapproval.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.Actor;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.ActorRole;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.ApproveCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.CreateClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.RejectClaimCommand;
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
import org.jfoundry.application.exception.NotFoundException;
import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseClaimCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");
    private static final UserId EMPLOYEE_ID = UserId.of("employee-1");
    private static final Actor EMPLOYEE = new Actor(EMPLOYEE_ID, ActorRole.EMPLOYEE);
    private static final Actor MANAGER = new Actor(UserId.of("manager-1"), ActorRole.MANAGER);
    private static final Actor FINANCE = new Actor(UserId.of("finance-1"), ActorRole.FINANCE);

    @Mock
    private ExpenseClaimRepository repository;

    private ExpenseClaimCommandService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseClaimCommandService(
                repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createBuildsAndSavesDraftOwnedByEmployee() {
        ExpenseClaimId id = service.create(new CreateClaimCommand(EMPLOYEE, "Customer visit"));

        ArgumentCaptor<ExpenseClaim> captor = ArgumentCaptor.forClass(ExpenseClaim.class);
        verify(repository).add(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(id);
        assertThat(captor.getValue().claimant()).isEqualTo(EMPLOYEE_ID);
        assertThat(captor.getValue().state()).isEqualTo(ClaimState.DRAFT);
    }

    @Test
    void managerApprovalLoadsAggregateInvokesBehaviorAndSavesOnce() {
        ExpenseClaim claim = submittedClaim("100.00");
        when(repository.findById(claim.id())).thenReturn(claim);

        service.approveByManager(new ApproveCommand(MANAGER, claim.id()));

        assertThat(claim.state()).isEqualTo(ClaimState.APPROVED);
        verify(repository).modify(claim);
    }

    @Test
    void missingClaimBecomesNotFound() {
        ExpenseClaimId id = ExpenseClaimId.of("missing");
        when(repository.findById(id)).thenReturn(null);

        assertThatThrownBy(() -> service.approveByManager(new ApproveCommand(MANAGER, id)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
        verify(repository, never()).modify(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void wrongRoleIsRejectedBeforeLoadingAggregate() {
        ExpenseClaimId id = ExpenseClaimId.of("claim-1");

        assertThatThrownBy(() -> service.approveByManager(new ApproveCommand(FINANCE, id)))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("MANAGER");
        verify(repository, never()).findById(id);
    }

    @Test
    void rejectionRoleMustMatchCurrentApprovalStage() {
        ExpenseClaim claim = submittedClaim("2000.01");
        when(repository.findById(claim.id())).thenReturn(claim);

        assertThatThrownBy(() -> service.reject(
                new RejectClaimCommand(FINANCE, claim.id(), "Missing receipt")))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("MANAGER");

        service.reject(new RejectClaimCommand(MANAGER, claim.id(), "Missing receipt"));
        assertThat(claim.state()).isEqualTo(ClaimState.REJECTED);
        verify(repository).modify(claim);
    }

    @Test
    void domainSelfApprovalRulePassesThroughWithoutSaving() {
        ExpenseClaim claim = submittedClaim("100.00");
        Actor claimantAsManager = new Actor(EMPLOYEE_ID, ActorRole.MANAGER);
        when(repository.findById(claim.id())).thenReturn(claim);

        assertThatThrownBy(() -> service.approveByManager(
                new ApproveCommand(claimantAsManager, claim.id())))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("own claim");
        verify(repository, never()).modify(claim);
    }

    private static ExpenseClaim submittedClaim(String amount) {
        ExpenseClaim claim = ExpenseClaim.draft(
                ExpenseClaimId.of("claim-1"), EMPLOYEE_ID, "Customer visit", NOW.minusSeconds(2));
        claim.addItem(
                EMPLOYEE_ID,
                new ExpenseItem(
                        ExpenseItemId.of("item-1"),
                        LocalDate.of(2026, 7, 10),
                        ExpenseCategory.TRAVEL,
                        Money.positiveCny(new BigDecimal(amount)),
                        "Taxi",
                        "receipt-1"),
                NOW.minusSeconds(1));
        claim.submit(EMPLOYEE_ID, NOW);
        return claim;
    }
}
