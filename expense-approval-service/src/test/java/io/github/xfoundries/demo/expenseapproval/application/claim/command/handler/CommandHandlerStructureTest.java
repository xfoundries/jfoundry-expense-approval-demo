package io.github.xfoundries.demo.expenseapproval.application.claim.command.handler;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import io.github.xfoundries.demo.expenseapproval.application.claim.command.AddExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByFinanceCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RejectExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RemoveExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ReopenExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.SubmitExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.UpdateExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.WithdrawExpenseClaimCommand;
import org.jfoundry.application.transaction.ApplicationTransactional;
import org.jfoundry.architecture.cqrs.Command;
import org.jfoundry.architecture.cqrs.CommandHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class CommandHandlerStructureTest {

    @ParameterizedTest
    @MethodSource("handlerCases")
    void eachCommandHasOneTransactionalHandler(HandlerCase handlerCase) throws Exception {
        assertThat(handlerCase.commandType()).hasAnnotation(Command.class);
        Method action = handlerCase.handlerType()
                .getDeclaredMethod(handlerCase.methodName(), handlerCase.commandType());
        assertThat(action.isAnnotationPresent(CommandHandler.class)).isTrue();
        assertThat(action.isAnnotationPresent(ApplicationTransactional.class))
                .isEqualTo(handlerCase.transactional());
    }

    private static Stream<HandlerCase> handlerCases() {
        return Stream.of(
                new HandlerCase("addItem", AddExpenseItemCommand.class, AddExpenseItemCommandHandler.class, true),
                new HandlerCase("updateItem", UpdateExpenseItemCommand.class, UpdateExpenseItemCommandHandler.class, true),
                new HandlerCase("removeItem", RemoveExpenseItemCommand.class, RemoveExpenseItemCommandHandler.class, true),
                new HandlerCase("submit", SubmitExpenseClaimCommand.class, SubmitExpenseClaimCommandHandler.class, true),
                new HandlerCase("approveByManager", ApproveExpenseClaimByManagerCommand.class,
                        ApproveExpenseClaimByManagerCommandHandler.class, false),
                new HandlerCase("approveByFinance", ApproveExpenseClaimByFinanceCommand.class,
                        ApproveExpenseClaimByFinanceCommandHandler.class, false),
                new HandlerCase("reject", RejectExpenseClaimCommand.class, RejectExpenseClaimCommandHandler.class, true),
                new HandlerCase("reopen", ReopenExpenseClaimCommand.class, ReopenExpenseClaimCommandHandler.class, true),
                new HandlerCase("withdraw", WithdrawExpenseClaimCommand.class, WithdrawExpenseClaimCommandHandler.class, true));
    }

    private record HandlerCase(String methodName, Class<?> commandType, Class<?> handlerType, boolean transactional) {
    }
}
