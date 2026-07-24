package io.github.xfoundries.demo.expenseapproval.boot;

import javax.sql.DataSource;

import io.github.xfoundries.demo.expenseapproval.support.PostgreSqlIntegrationTest;
import org.jfoundry.application.transaction.TransactionRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationSmokeTest extends PostgreSqlIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContextStartsWithDataSource() {
        assertThat(dataSource).isNotNull();
        assertThat(applicationContext.getBeansOfType(TransactionRunner.class)).hasSize(1);
    }
}
