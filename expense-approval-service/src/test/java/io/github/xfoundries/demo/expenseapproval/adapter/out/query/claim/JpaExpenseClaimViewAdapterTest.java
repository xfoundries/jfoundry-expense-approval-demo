package io.github.xfoundries.demo.expenseapproval.adapter.out.query.claim;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PaymentDisplayStatus;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.port.out.ExpenseClaimViewPort;
import io.github.xfoundries.demo.expenseapproval.boot.ExpenseApprovalApplication;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseCategory;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import io.github.xfoundries.demo.expenseapproval.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExpenseApprovalApplication.class)
@Transactional
class JpaExpenseClaimViewAdapterTest extends PostgreSqlIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");
    private static final UserId EMPLOYEE_1 = UserId.of("employee-1");
    private static final UserId EMPLOYEE_2 = UserId.of("employee-2");
    private static final UserId MANAGER = UserId.of("manager-1");

    @Autowired ExpenseClaimRepository repository;
    @Autowired ExpenseClaimViewPort viewPort;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void ownedClaimsCanBeFilteredAndPaged() {
        repository.add(draft("claim-1", EMPLOYEE_1, "Draft"));
        repository.add(submitted("claim-2", EMPLOYEE_1, "Small", "100.00"));
        repository.add(submitted("claim-3", EMPLOYEE_2, "Other owner", "100.00"));

        PageResult<ClaimSummary> page = viewPort.findOwned(
                EMPLOYEE_1, ClaimState.PENDING_MANAGER, new PageQuery(0, 1));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).extracting(ClaimSummary::id).containsExactly("claim-2");
    }

    @Test
    void approvalQueuesContainOnlyTheirCurrentStage() {
        ExpenseClaim managerPending = submitted("claim-manager", EMPLOYEE_1, "Small", "100.00");
        ExpenseClaim financePending = submitted("claim-finance", EMPLOYEE_2, "Large", "2000.01");
        financePending.approveByManager(MANAGER, NOW.plusSeconds(3));
        repository.add(managerPending);
        repository.add(financePending);

        assertThat(viewPort.findPendingManager(new PageQuery(0, 20)).items())
                .extracting(ClaimSummary::id).containsExactly("claim-manager");
        assertThat(viewPort.findPendingFinance(new PageQuery(0, 20)).items())
                .extracting(ClaimSummary::id).containsExactly("claim-finance");
    }

    @Test
    void detailContainsItemsAndOrderedActionHistory() {
        ExpenseClaim claim = submitted("claim-detail", EMPLOYEE_1, "Large", "2000.01");
        claim.approveByManager(MANAGER, NOW.plusSeconds(3));
        repository.add(claim);

        ClaimDetail detail = viewPort.findDetail(claim.id()).orElseThrow();

        assertThat(detail.total()).isEqualByComparingTo("2000.01");
        assertThat(detail.items()).hasSize(1);
        assertThat(detail.actions()).extracting(action -> action.type())
                .containsExactly("SUBMITTED", "MANAGER_APPROVED");
    }

    @Test
    void queryDerivesPaymentStateAndIncludesProjectedResult() {
        ExpenseClaim draft = draft("claim-draft", EMPLOYEE_1, "Draft");
        ExpenseClaim pending = approved("claim-pending", "100.00");
        ExpenseClaim paid = approved("claim-paid", "200.00");
        ExpenseClaim failed = approved("claim-failed", "300.00");
        repository.add(draft);
        repository.add(pending);
        repository.add(paid);
        repository.add(failed);
        insertPayment("claim-paid", "PAID", "payment-1", null, null, "paid-event");
        insertPayment("claim-failed", "FAILED", null, "LIMIT", "Payment limit exceeded", "failed-event");

        Map<String, ClaimSummary> summaries = viewPort.findOwned(
                        EMPLOYEE_1, null, new PageQuery(0, 20)).items().stream()
                .collect(Collectors.toMap(ClaimSummary::id, Function.identity()));

        assertThat(summaries.get("claim-draft").payment().status())
                .isEqualTo(PaymentDisplayStatus.NOT_APPLICABLE);
        assertThat(summaries.get("claim-pending").payment().status())
                .isEqualTo(PaymentDisplayStatus.PENDING);
        assertThat(summaries.get("claim-paid").payment().status())
                .isEqualTo(PaymentDisplayStatus.PAID);
        assertThat(summaries.get("claim-failed").payment().status())
                .isEqualTo(PaymentDisplayStatus.FAILED);

        ClaimDetail detail = viewPort.findDetail(ExpenseClaimId.of("claim-failed")).orElseThrow();
        assertThat(detail.payment().failureCode()).isEqualTo("LIMIT");
        assertThat(detail.payment().failureReason()).isEqualTo("Payment limit exceeded");
    }

    private static ExpenseClaim draft(String id, UserId claimant, String title) {
        return ExpenseClaim.draft(ExpenseClaimId.of(id), claimant, title, NOW);
    }

    private static ExpenseClaim submitted(
            String id, UserId claimant, String title, String amount) {
        ExpenseClaim claim = draft(id, claimant, title);
        claim.addItem(
                claimant,
                new ExpenseItem(
                        ExpenseItemId.of(id + "-item"),
                        LocalDate.of(2026, 7, 10),
                        ExpenseCategory.TRAVEL,
                        Money.positiveCny(new BigDecimal(amount)),
                        "Taxi",
                        "receipt-1"),
                NOW.plusSeconds(1));
        claim.submit(claimant, NOW.plusSeconds(2));
        return claim;
    }

    private static ExpenseClaim approved(String id, String amount) {
        ExpenseClaim claim = submitted(id, EMPLOYEE_1, "Approved", amount);
        claim.approveByManager(MANAGER, NOW.plusSeconds(3));
        return claim;
    }

    private void insertPayment(
            String claimId,
            String status,
            String reference,
            String failureCode,
            String failureReason,
            String eventId) {
        OffsetDateTime processedAt = NOW.plusSeconds(10).atOffset(ZoneOffset.UTC);
        jdbcTemplate.update("""
                insert into claim_payment_status(
                    claim_id, status, payment_reference, failure_code, failure_reason,
                    processed_at, source_event_id, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, claimId, status, reference, failureCode, failureReason,
                processedAt, eventId, processedAt);
    }
}
