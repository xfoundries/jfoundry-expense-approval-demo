package io.github.xfoundries.demo.expenseapproval.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("io.github.xfoundries.demo.expenseapproval.adapter.out")
@SpringBootApplication(scanBasePackages = "io.github.xfoundries.demo.expenseapproval")
public class ExpenseApprovalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseApprovalApplication.class, args);
    }
}
