package io.github.xfoundries.demo.expenseapproval.application.claim.command;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

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
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ClaimCommandDispatcher;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import org.jfoundry.architecture.cqrs.CommandDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimCommandDispatcherTest {

    @Mock CreateExpenseClaimCommandHandler createHandler;
    @Mock AddExpenseItemCommandHandler addItemHandler;
    @Mock UpdateExpenseItemCommandHandler updateItemHandler;
    @Mock RemoveExpenseItemCommandHandler removeItemHandler;
    @Mock SubmitExpenseClaimCommandHandler submitHandler;
    @Mock ApproveExpenseClaimByManagerCommandHandler managerApprovalHandler;
    @Mock ApproveExpenseClaimByFinanceCommandHandler financeApprovalHandler;
    @Mock RejectExpenseClaimCommandHandler rejectHandler;
    @Mock ReopenExpenseClaimCommandHandler reopenHandler;
    @Mock WithdrawExpenseClaimCommandHandler withdrawHandler;

    @Mock CreateExpenseClaimCommand create;
    @Mock AddExpenseItemCommand addItem;
    @Mock UpdateExpenseItemCommand updateItem;
    @Mock RemoveExpenseItemCommand removeItem;
    @Mock SubmitExpenseClaimCommand submit;
    @Mock ApproveExpenseClaimByManagerCommand managerApproval;
    @Mock ApproveExpenseClaimByFinanceCommand financeApproval;
    @Mock RejectExpenseClaimCommand reject;
    @Mock ReopenExpenseClaimCommand reopen;
    @Mock WithdrawExpenseClaimCommand withdraw;

    private ClaimCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new DefaultClaimCommandDispatcher(
                createHandler, addItemHandler, updateItemHandler, removeItemHandler,
                submitHandler, managerApprovalHandler, financeApprovalHandler,
                rejectHandler, reopenHandler, withdrawHandler);
    }

    @Test
    void exposesOnlyExplicitAnnotatedOverloadsWithoutRuntimeRegistry() {
        assertThat(ClaimCommandDispatcher.class.isInterface()).isTrue();
        Class<?>[] commands = {
                CreateExpenseClaimCommand.class,
                AddExpenseItemCommand.class,
                UpdateExpenseItemCommand.class,
                RemoveExpenseItemCommand.class,
                SubmitExpenseClaimCommand.class,
                ApproveExpenseClaimByManagerCommand.class,
                ApproveExpenseClaimByFinanceCommand.class,
                RejectExpenseClaimCommand.class,
                ReopenExpenseClaimCommand.class,
                WithdrawExpenseClaimCommand.class
        };
        assertThat(commands).allSatisfy(commandType -> {
            Method method = findDispatch(commandType);
            assertThat(method.isAnnotationPresent(CommandDispatcher.class)).isTrue();
        });
        assertThat(Arrays.stream(DefaultClaimCommandDispatcher.class.getDeclaredFields())
                .map(field -> field.getType()))
                .noneMatch(Map.class::isAssignableFrom);
        assertThat(Arrays.stream(ClaimCommandDispatcher.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("dispatch")))
                .allMatch(method -> method.getParameterTypes()[0] != Object.class);
    }

    @Test
    void routesEveryCommandToItsTypedHandler() {
        ExpenseClaimId claimId = ExpenseClaimId.of("claim-1");
        ExpenseItemId itemId = ExpenseItemId.of("item-1");
        when(createHandler.handle(create)).thenReturn(claimId);
        when(addItemHandler.handle(addItem)).thenReturn(itemId);

        assertThat(dispatcher.dispatch(create)).isEqualTo(claimId);
        assertThat(dispatcher.dispatch(addItem)).isEqualTo(itemId);
        dispatcher.dispatch(updateItem);
        dispatcher.dispatch(removeItem);
        dispatcher.dispatch(submit);
        dispatcher.dispatch(managerApproval);
        dispatcher.dispatch(financeApproval);
        dispatcher.dispatch(reject);
        dispatcher.dispatch(reopen);
        dispatcher.dispatch(withdraw);

        verify(createHandler).handle(create);
        verify(addItemHandler).handle(addItem);
        verify(updateItemHandler).handle(updateItem);
        verify(removeItemHandler).handle(removeItem);
        verify(submitHandler).handle(submit);
        verify(managerApprovalHandler).handle(managerApproval);
        verify(financeApprovalHandler).handle(financeApproval);
        verify(rejectHandler).handle(reject);
        verify(reopenHandler).handle(reopen);
        verify(withdrawHandler).handle(withdraw);
    }

    private static Method findDispatch(Class<?> commandType) {
        try {
            return ClaimCommandDispatcher.class.getDeclaredMethod("dispatch", commandType);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("Missing dispatcher overload for " + commandType.getSimpleName(), exception);
        }
    }
}
