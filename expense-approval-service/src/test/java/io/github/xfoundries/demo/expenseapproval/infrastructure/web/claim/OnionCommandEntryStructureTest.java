package io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim;

import java.util.Arrays;
import java.util.Set;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.AddExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ApproveExpenseClaimByFinanceCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ApproveExpenseClaimByManagerCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.CreateExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.RejectExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.RemoveExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.ReopenExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.SubmitExpenseClaimCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.UpdateExpenseItemCommandHandler;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.handler.WithdrawExpenseClaimCommandHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OnionCommandEntryStructureTest {

    @Test
    void controllerInjectsBusinessCommandHandlersDirectly() {
        Set<Class<?>> commandEntryTypes = Set.of(
                CreateExpenseClaimCommandHandler.class,
                AddExpenseItemCommandHandler.class,
                UpdateExpenseItemCommandHandler.class,
                RemoveExpenseItemCommandHandler.class,
                SubmitExpenseClaimCommandHandler.class,
                ApproveExpenseClaimByManagerCommandHandler.class,
                ApproveExpenseClaimByFinanceCommandHandler.class,
                RejectExpenseClaimCommandHandler.class,
                ReopenExpenseClaimCommandHandler.class,
                WithdrawExpenseClaimCommandHandler.class);

        Set<Class<?>> controllerDependencies = Arrays.stream(ExpenseClaimController.class.getDeclaredFields())
                .map(field -> field.getType())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(controllerDependencies).containsAll(commandEntryTypes);
        assertThat(controllerDependencies)
                .noneMatch(type -> type.getSimpleName().contains("CommandDispatcher"));
    }
}
