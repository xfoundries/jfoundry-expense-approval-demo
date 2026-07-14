package io.github.xfoundries.demo.expenseapproval.boot.config;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.DefaultClaimCommandDispatcher;
import io.github.xfoundries.demo.expenseapproval.application.approval.FinalApprovalCoordinator;
import io.github.xfoundries.demo.expenseapproval.domain.policy.MonthlyExpenseLimitPolicy;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.AddExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ApproveExpenseClaimByFinanceCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ApproveExpenseClaimByManagerCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ClaimCommandContext;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.CreateExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.RejectExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.RemoveExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ReopenExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.SubmitExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.UpdateExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.WithdrawExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in.ClaimCommandDispatcher;
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
    ClaimCommandContext claimCommandContext(
            ExpenseClaimRepository repository, Clock applicationClock) {
        return new ClaimCommandContext(repository, applicationClock);
    }

    @Bean
    CreateExpenseClaimCommandHandler createExpenseClaimCommandHandler(
            ExpenseClaimRepository repository, Clock applicationClock) {
        return new CreateExpenseClaimCommandHandler(repository, applicationClock);
    }

    @Bean
    AddExpenseItemCommandHandler addExpenseItemCommandHandler(ClaimCommandContext context) {
        return new AddExpenseItemCommandHandler(context);
    }

    @Bean
    UpdateExpenseItemCommandHandler updateExpenseItemCommandHandler(ClaimCommandContext context) {
        return new UpdateExpenseItemCommandHandler(context);
    }

    @Bean
    RemoveExpenseItemCommandHandler removeExpenseItemCommandHandler(ClaimCommandContext context) {
        return new RemoveExpenseItemCommandHandler(context);
    }

    @Bean
    SubmitExpenseClaimCommandHandler submitExpenseClaimCommandHandler(ClaimCommandContext context) {
        return new SubmitExpenseClaimCommandHandler(context);
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
    RejectExpenseClaimCommandHandler rejectExpenseClaimCommandHandler(ClaimCommandContext context) {
        return new RejectExpenseClaimCommandHandler(context);
    }

    @Bean
    ReopenExpenseClaimCommandHandler reopenExpenseClaimCommandHandler(ClaimCommandContext context) {
        return new ReopenExpenseClaimCommandHandler(context);
    }

    @Bean
    WithdrawExpenseClaimCommandHandler withdrawExpenseClaimCommandHandler(ClaimCommandContext context) {
        return new WithdrawExpenseClaimCommandHandler(context);
    }

    @Bean
    ClaimCommandDispatcher claimCommandDispatcher(
            CreateExpenseClaimCommandHandler createHandler,
            AddExpenseItemCommandHandler addItemHandler,
            UpdateExpenseItemCommandHandler updateItemHandler,
            RemoveExpenseItemCommandHandler removeItemHandler,
            SubmitExpenseClaimCommandHandler submitHandler,
            ApproveExpenseClaimByManagerCommandHandler managerApprovalHandler,
            ApproveExpenseClaimByFinanceCommandHandler financeApprovalHandler,
            RejectExpenseClaimCommandHandler rejectHandler,
            ReopenExpenseClaimCommandHandler reopenHandler,
            WithdrawExpenseClaimCommandHandler withdrawHandler) {
        return new DefaultClaimCommandDispatcher(
                createHandler, addItemHandler, updateItemHandler, removeItemHandler,
                submitHandler, managerApprovalHandler, financeApprovalHandler,
                rejectHandler, reopenHandler, withdrawHandler);
    }

    @Bean
    ExpenseClaimQueryUseCase expenseClaimQueryUseCase(ExpenseClaimViewPort viewPort) {
        return new ExpenseClaimQueryService(viewPort);
    }

    @Bean
    PaymentResultProjector paymentResultProjector(
            InboxTemplate inboxTemplate,
            TransactionRunner transactionRunner,
            PaymentStatusProjectionPort projectionPort) {
        return new PaymentResultProjector(inboxTemplate, transactionRunner, projectionPort);
    }
}
