package io.github.xfoundries.demo.expenseapproval.infrastructure.query.claim;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.application.port.out.ExpenseClaimQueryPort;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExpenseApprovalApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:expense-query;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always"
})
@Transactional
class ExpenseClaimQueryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");
    private static final UserId EMPLOYEE_1 = UserId.of("employee-1");
    private static final UserId EMPLOYEE_2 = UserId.of("employee-2");
    private static final UserId MANAGER = UserId.of("manager-1");

    @Autowired ExpenseClaimRepository repository;
    @Autowired ExpenseClaimQueryPort queryPort;

    @Test
    void ownedClaimsCanBeFilteredAndPaged() {
        repository.add(draft("claim-1", EMPLOYEE_1, "Draft"));
        repository.add(submitted("claim-2", EMPLOYEE_1, "Small", "100.00"));
        repository.add(submitted("claim-3", EMPLOYEE_2, "Other owner", "100.00"));

        PageResult<ClaimSummary> page = queryPort.findOwned(
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

        assertThat(queryPort.findPendingManager(new PageQuery(0, 20)).items())
                .extracting(ClaimSummary::id).containsExactly("claim-manager");
        assertThat(queryPort.findPendingFinance(new PageQuery(0, 20)).items())
                .extracting(ClaimSummary::id).containsExactly("claim-finance");
    }

    @Test
    void detailContainsItemsAndOrderedActionHistory() {
        ExpenseClaim claim = submitted("claim-detail", EMPLOYEE_1, "Large", "2000.01");
        claim.approveByManager(MANAGER, NOW.plusSeconds(3));
        repository.add(claim);

        ClaimDetail detail = queryPort.findDetail(claim.id()).orElseThrow();

        assertThat(detail.total()).isEqualByComparingTo("2000.01");
        assertThat(detail.items()).hasSize(1);
        assertThat(detail.actions()).extracting(action -> action.type())
                .containsExactly("SUBMITTED", "MANAGER_APPROVED");
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
}
