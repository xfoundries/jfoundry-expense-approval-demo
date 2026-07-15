package io.github.xfoundries.demo.expenseapproval.infrastructure.lookup.claim;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import io.github.xfoundries.demo.expenseapproval.application.approval.ApprovedExpenseAmountReader;
import io.github.xfoundries.demo.expenseapproval.boot.ExpenseApprovalApplication;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExpenseApprovalApplication.class)
@Transactional
class MybatisApprovedExpenseAmountReaderTest extends PostgreSqlIntegrationTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ApprovedExpenseAmountReader amountReader;

    @Test
    void sumsOnlyApprovedClaimsForTheEmployeeInsideTheHalfOpenRange() {
        Instant from = Instant.parse("2026-07-31T16:00:00Z");
        Instant to = Instant.parse("2026-08-31T16:00:00Z");
        insert("approved-1", "employee-1", "APPROVED", "100.00", from.plusSeconds(1));
        insert("approved-2", "employee-1", "APPROVED", "200.00", to.minusSeconds(1));
        insert("pending", "employee-1", "PENDING_MANAGER", "900.00", from.plusSeconds(2));
        insert("other", "employee-2", "APPROVED", "800.00", from.plusSeconds(3));
        insert("at-end", "employee-1", "APPROVED", "700.00", to);

        Money total = amountReader.totalApproved(UserId.of("employee-1"), from, to);

        assertThat(total).isEqualTo(Money.cny("300.00"));
    }

    private void insert(String id, String employee, String state, String amount, Instant completedAt) {
        OffsetDateTime timestamp = completedAt.atOffset(ZoneOffset.UTC);
        jdbcTemplate.update("""
                insert into expense_claim(
                    id, claimant_id, title, state, total_amount, finance_approval_required,
                    created_at, updated_at, submitted_at, completed_at, version)
                values (?, ?, 'Test', ?, ?::numeric, false, ?, ?, ?, ?, 0)
                """, id, employee, state, amount, timestamp, timestamp, timestamp, timestamp);
    }
}
