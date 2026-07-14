package io.github.xfoundries.demo.expenseapproval.boot;

import org.flywaydb.core.Flyway;
import io.github.xfoundries.demo.expenseapproval.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ExpenseApprovalApplication.class,
        properties = "spring.sql.init.mode=never")
class PostgreSqlMigrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void appliesApplicationOwnedAggregateAndMessagingMigrations() {
        assertThat(flyway.info().applied()).hasSize(2);
        assertThat(tableExists("expense_claim")).isTrue();
        assertThat(tableExists("jfoundry_outbox_event")).isTrue();
        assertThat(tableExists("jfoundry_inbox_message")).isTrue();
        assertThat(tableExists("claim_payment_status")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = current_schema() and table_name = ?",
                Integer.class,
                tableName);
        return count != null && count == 1;
    }
}
