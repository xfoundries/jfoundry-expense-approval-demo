package io.github.xfoundries.demo.expenseapproval.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jfoundry.domain.exception.DomainStateException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseClaimTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");
    private static final UserId EMPLOYEE = UserId.of("employee-1");
    private static final UserId OTHER_EMPLOYEE = UserId.of("employee-2");
    private static final UserId MANAGER = UserId.of("manager-1");
    private static final UserId FINANCE = UserId.of("finance-1");

    @Test
    void domainAggregateDoesNotContainPersistenceVersion() {
        assertThat(Arrays.stream(ExpenseClaim.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("version");
    }

    @Test
    void expenseItemAmountMustBePositiveCny() {
        assertThatThrownBy(() -> Money.positiveCny(BigDecimal.ZERO))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void totalIsCalculatedFromItems() {
        ExpenseClaim claim = draft();

        claim.addItem(EMPLOYEE, item("item-1", "120.50"), NOW.plusSeconds(1));
        claim.addItem(EMPLOYEE, item("item-2", "79.50"), NOW.plusSeconds(2));

        assertThat(claim.total()).isEqualTo(Money.cny("200.00"));
    }

    @Test
    void onlyClaimantCanEditDraft() {
        ExpenseClaim claim = draft();

        assertThatThrownBy(() -> claim.addItem(OTHER_EMPLOYEE, item("10.00"), NOW))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("claimant");
    }

    @Test
    void claimMustContainAnItemBeforeSubmission() {
        ExpenseClaim claim = draft();

        assertThatThrownBy(() -> claim.submit(EMPLOYEE, NOW.plusSeconds(1)))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("item");
    }

    @Test
    void smallClaimIsApprovedByManager() {
        ExpenseClaim claim = submitted("2000.00");

        claim.approveByManager(MANAGER, NOW.plusSeconds(2));

        assertThat(claim.state()).isEqualTo(ClaimState.APPROVED);
        assertThat(claim.completedAt()).contains(NOW.plusSeconds(2));
        assertThat(claim.actions()).extracting(ClaimAction::type)
                .containsExactly(ClaimActionType.SUBMITTED, ClaimActionType.MANAGER_APPROVED);
    }

    @Test
    void highValueClaimRequiresManagerThenFinance() {
        ExpenseClaim claim = submitted("2000.01");

        claim.approveByManager(MANAGER, NOW.plusSeconds(2));
        assertThat(claim.state()).isEqualTo(ClaimState.PENDING_FINANCE);

        claim.approveByFinance(FINANCE, NOW.plusSeconds(3));
        assertThat(claim.state()).isEqualTo(ClaimState.APPROVED);
        assertThat(claim.actions()).extracting(ClaimAction::resultingState)
                .containsExactly(
                        ClaimState.PENDING_MANAGER,
                        ClaimState.PENDING_FINANCE,
                        ClaimState.APPROVED);
    }

    @Test
    void currentApproverCanRejectWithReason() {
        ExpenseClaim claim = submitted("2000.01");
        claim.approveByManager(MANAGER, NOW.plusSeconds(2));

        claim.reject(FINANCE, RejectionReason.of("Missing receipt"), NOW.plusSeconds(3));

        assertThat(claim.state()).isEqualTo(ClaimState.REJECTED);
        assertThat(claim.actions().getLast().reason()).contains("Missing receipt");
    }

    @Test
    void claimantCanReopenRejectedClaimAndSubmitAgain() {
        ExpenseClaim claim = submitted("100.00");
        claim.reject(MANAGER, RejectionReason.of("Wrong category"), NOW.plusSeconds(2));

        claim.reopen(EMPLOYEE, NOW.plusSeconds(3));
        claim.updateItem(EMPLOYEE, item("120.00"), NOW.plusSeconds(4));
        claim.submit(EMPLOYEE, NOW.plusSeconds(5));

        assertThat(claim.state()).isEqualTo(ClaimState.PENDING_MANAGER);
        assertThat(claim.actions()).extracting(ClaimAction::type)
                .containsExactly(
                        ClaimActionType.SUBMITTED,
                        ClaimActionType.REJECTED,
                        ClaimActionType.REOPENED,
                        ClaimActionType.SUBMITTED);
    }

    @Test
    void claimantCanWithdrawBeforeFinalApproval() {
        ExpenseClaim claim = submitted("2000.01");
        claim.approveByManager(MANAGER, NOW.plusSeconds(2));

        claim.withdraw(EMPLOYEE, NOW.plusSeconds(3));

        assertThat(claim.state()).isEqualTo(ClaimState.WITHDRAWN);
        assertThat(claim.completedAt()).contains(NOW.plusSeconds(3));
    }

    @Test
    void claimantCannotApproveOwnClaim() {
        ExpenseClaim claim = submitted("100.00");

        assertThatThrownBy(() -> claim.approveByManager(EMPLOYEE, NOW.plusSeconds(2)))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("own claim");
    }

    @Test
    void approvalStagesCannotBeSkippedOrRepeated() {
        ExpenseClaim claim = submitted("2000.01");

        assertThatThrownBy(() -> claim.approveByFinance(FINANCE, NOW.plusSeconds(2)))
                .isInstanceOf(DomainStateException.class);

        claim.approveByManager(MANAGER, NOW.plusSeconds(2));
        assertThatThrownBy(() -> claim.approveByManager(MANAGER, NOW.plusSeconds(3)))
                .isInstanceOf(DomainStateException.class);
    }

    @Test
    void approvedAndWithdrawnClaimsAreTerminal() {
        ExpenseClaim approved = submitted("100.00");
        approved.approveByManager(MANAGER, NOW.plusSeconds(2));
        ExpenseClaim withdrawn = submitted("100.00");
        withdrawn.withdraw(EMPLOYEE, NOW.plusSeconds(2));

        assertThatThrownBy(() -> approved.reopen(EMPLOYEE, NOW.plusSeconds(3)))
                .isInstanceOf(DomainStateException.class);
        assertThatThrownBy(() -> withdrawn.submit(EMPLOYEE, NOW.plusSeconds(3)))
                .isInstanceOf(DomainStateException.class);
    }

    private static ExpenseClaim draft() {
        return ExpenseClaim.draft(
                ExpenseClaimId.generate(), EMPLOYEE, "Customer visit", NOW);
    }

    private static ExpenseClaim submitted(String amount) {
        ExpenseClaim claim = draft();
        claim.addItem(EMPLOYEE, item(amount), NOW.plusMillis(1));
        claim.submit(EMPLOYEE, NOW.plusSeconds(1));
        return claim;
    }

    private static ExpenseItem item(String amount) {
        return item("item-1", amount);
    }

    private static ExpenseItem item(String id, String amount) {
        return new ExpenseItem(
                ExpenseItemId.of(id),
                LocalDate.of(2026, 7, 10),
                ExpenseCategory.TRAVEL,
                Money.positiveCny(new BigDecimal(amount)),
                "Taxi",
                "receipt-1");
    }
}
