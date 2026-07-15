package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.CreateExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.Command;
import org.jfoundry.architecture.cqrs.CommandHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateExpenseClaimCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");

    @Mock
    private ExpenseClaimRepository repository;

    @Test
    void commandAndHandlerExpressCqrsAndTransactionBoundary() throws Exception {
        assertThat(CreateExpenseClaimCommand.class).hasAnnotation(Command.class);
        Method create = CreateExpenseClaimCommandHandler.class
                .getDeclaredMethod("create", CreateExpenseClaimCommand.class);
        assertThat(create.isAnnotationPresent(CommandHandler.class)).isTrue();
        assertThat(create.isAnnotationPresent(ApplicationTransactional.class)).isTrue();
    }

    @Test
    void createsAndSavesDraftOwnedByEmployee() {
        CreateExpenseClaimCommandHandler handler = new CreateExpenseClaimCommandHandler(
                repository, Clock.fixed(NOW, ZoneOffset.UTC));
        ApprovalActor employee = new ApprovalActor(UserId.of("employee-1"), ApprovalRole.EMPLOYEE);

        ExpenseClaimId id = handler.create(new CreateExpenseClaimCommand(employee, "Customer visit"));

        ArgumentCaptor<ExpenseClaim> captor = ArgumentCaptor.forClass(ExpenseClaim.class);
        verify(repository).add(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(id);
        assertThat(captor.getValue().claimant()).isEqualTo(employee.userId());
        assertThat(captor.getValue().state()).isEqualTo(ClaimState.DRAFT);
    }
}
