package io.github.xfoundries.demo.expenseapproval.boot.config;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

import io.github.xfoundries.demo.expenseapproval.application.approval.FinalApprovalCoordinator;
import io.github.xfoundries.demo.expenseapproval.domain.policy.MonthlyExpenseLimitPolicy;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.AddExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ApproveExpenseClaimByFinanceCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ApproveExpenseClaimByManagerCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ExpenseClaimCommandSupport;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.CreateExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.RejectExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.RemoveExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ReopenExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.SubmitExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.UpdateExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.WithdrawExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.port.in.ExpenseClaimQueryUseCase;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.port.out.ExpenseClaimViewPort;
import io.github.xfoundries.demo.expenseapproval.application.approval.port.out.ApprovedExpenseAmountPort;
import io.github.xfoundries.demo.expenseapproval.application.approval.ExpenseClaimApprovedTranslator;
import io.github.xfoundries.demo.expenseapproval.application.payment.PaymentResultProjector;
import io.github.xfoundries.demo.expenseapproval.application.payment.port.out.PaymentStatusProjectionPort;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ExpenseClaimQueryService;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import org.jfoundry.application.lock.LockTemplate;
import org.jfoundry.application.inbox.InboxTemplate;
import org.jfoundry.application.outbox.OutboxTemplate;
import org.jfoundry.application.transaction.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    ExpenseClaimCommandSupport expenseClaimCommandSupport(
            ExpenseClaimRepository repository, Clock applicationClock, TransactionRunner transactionRunner) {
        return new ExpenseClaimCommandSupport(repository, applicationClock, transactionRunner);
    }

    @Bean
    CreateExpenseClaimCommandHandler createExpenseClaimCommandHandler(
            ExpenseClaimRepository repository, Clock applicationClock, TransactionRunner transactionRunner) {
        return new CreateExpenseClaimCommandHandler(repository, applicationClock, transactionRunner);
    }

    @Bean
    AddExpenseItemCommandHandler addExpenseItemCommandHandler(ExpenseClaimCommandSupport support) {
        return new AddExpenseItemCommandHandler(support);
    }

    @Bean
    UpdateExpenseItemCommandHandler updateExpenseItemCommandHandler(ExpenseClaimCommandSupport support) {
        return new UpdateExpenseItemCommandHandler(support);
    }

    @Bean
    RemoveExpenseItemCommandHandler removeExpenseItemCommandHandler(ExpenseClaimCommandSupport support) {
        return new RemoveExpenseItemCommandHandler(support);
    }

    @Bean
    SubmitExpenseClaimCommandHandler submitExpenseClaimCommandHandler(ExpenseClaimCommandSupport support) {
        return new SubmitExpenseClaimCommandHandler(support);
    }

    @Bean
    ApproveExpenseClaimByManagerCommandHandler approveExpenseClaimByManagerCommandHandler(
            FinalApprovalCoordinator coordinator) {
        return new ApproveExpenseClaimByManagerCommandHandler(coordinator);
    }

    @Bean
    ApproveExpenseClaimByFinanceCommandHandler approveExpenseClaimByFinanceCommandHandler(
            FinalApprovalCoordinator coordinator) {
        return new ApproveExpenseClaimByFinanceCommandHandler(coordinator);
    }

    @Bean
    FinalApprovalCoordinator finalApprovalCoordinator(
            ExpenseClaimRepository repository,
            ApprovedExpenseAmountPort approvedExpenseAmountPort,
            LockTemplate lockTemplate,
            TransactionRunner transactionRunner,
            OutboxTemplate outboxTemplate,
            Clock applicationClock) {
        return new FinalApprovalCoordinator(
                repository,
                approvedExpenseAmountPort,
                new MonthlyExpenseLimitPolicy(Money.cny("10000.00")),
                lockTemplate,
                transactionRunner,
                outboxTemplate,
                new ExpenseClaimApprovedTranslator(),
                applicationClock,
                ZoneId.of("Asia/Shanghai"),
                Duration.ofSeconds(2));
    }

    @Bean
    RejectExpenseClaimCommandHandler rejectExpenseClaimCommandHandler(ExpenseClaimCommandSupport support) {
        return new RejectExpenseClaimCommandHandler(support);
    }

    @Bean
    ReopenExpenseClaimCommandHandler reopenExpenseClaimCommandHandler(ExpenseClaimCommandSupport support) {
        return new ReopenExpenseClaimCommandHandler(support);
    }

    @Bean
    WithdrawExpenseClaimCommandHandler withdrawExpenseClaimCommandHandler(ExpenseClaimCommandSupport support) {
        return new WithdrawExpenseClaimCommandHandler(support);
    }

    @Bean
    ExpenseClaimQueryUseCase expenseClaimQueryUseCase(ExpenseClaimViewPort viewPort) {
        return new ExpenseClaimQueryService(viewPort);
    }

    @Bean
    PaymentResultProjector paymentResultProjector(
            InboxTemplate inboxTemplate,
            PaymentStatusProjectionPort projectionPort) {
        return new PaymentResultProjector(inboxTemplate, projectionPort);
    }
}
