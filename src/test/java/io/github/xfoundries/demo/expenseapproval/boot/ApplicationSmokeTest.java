package io.github.xfoundries.demo.expenseapproval.boot;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationSmokeTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void applicationContextStartsWithDataSource() {
        assertThat(dataSource).isNotNull();
    }
}
