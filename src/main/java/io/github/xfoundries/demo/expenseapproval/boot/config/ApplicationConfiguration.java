package io.github.xfoundries.demo.expenseapproval.boot.config;

import java.time.Clock;

import io.github.xfoundries.demo.expenseapproval.application.port.in.ExpenseClaimCommandUseCase;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ExpenseClaimQueryUseCase;
import io.github.xfoundries.demo.expenseapproval.application.port.out.ExpenseClaimQueryPort;
import io.github.xfoundries.demo.expenseapproval.application.service.ExpenseClaimCommandService;
import io.github.xfoundries.demo.expenseapproval.application.service.ExpenseClaimQueryService;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    ExpenseClaimCommandUseCase expenseClaimCommandUseCase(
            ExpenseClaimRepository repository, Clock applicationClock) {
        return new ExpenseClaimCommandService(repository, applicationClock);
    }

    @Bean
    ExpenseClaimQueryUseCase expenseClaimQueryUseCase(ExpenseClaimQueryPort queryPort) {
        return new ExpenseClaimQueryService(queryPort);
    }
}

