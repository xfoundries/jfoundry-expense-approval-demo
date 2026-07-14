package io.github.xfoundries.demo.expenseapproval.application.command.handler;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import io.github.xfoundries.demo.expenseapproval.application.command.AddExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.ApproveExpenseClaimByFinanceCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.RejectExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.RemoveExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.ReopenExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.SubmitExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.UpdateExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.WithdrawExpenseClaimCommand;
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
        Method handle = handlerCase.handlerType().getDeclaredMethod("handle", handlerCase.commandType());
        assertThat(handle.isAnnotationPresent(CommandHandler.class)).isTrue();
        assertThat(handle.isAnnotationPresent(ApplicationTransactional.class))
                .isEqualTo(handlerCase.transactional());
    }

    private static Stream<HandlerCase> handlerCases() {
        return Stream.of(
                new HandlerCase(AddExpenseItemCommand.class, AddExpenseItemCommandHandler.class, true),
                new HandlerCase(UpdateExpenseItemCommand.class, UpdateExpenseItemCommandHandler.class, true),
                new HandlerCase(RemoveExpenseItemCommand.class, RemoveExpenseItemCommandHandler.class, true),
                new HandlerCase(SubmitExpenseClaimCommand.class, SubmitExpenseClaimCommandHandler.class, true),
                new HandlerCase(ApproveExpenseClaimByManagerCommand.class,
                        ApproveExpenseClaimByManagerCommandHandler.class, false),
                new HandlerCase(ApproveExpenseClaimByFinanceCommand.class,
                        ApproveExpenseClaimByFinanceCommandHandler.class, false),
                new HandlerCase(RejectExpenseClaimCommand.class, RejectExpenseClaimCommandHandler.class, true),
                new HandlerCase(ReopenExpenseClaimCommand.class, ReopenExpenseClaimCommandHandler.class, true),
                new HandlerCase(WithdrawExpenseClaimCommand.class, WithdrawExpenseClaimCommandHandler.class, true));
    }

    private record HandlerCase(Class<?> commandType, Class<?> handlerType, boolean transactional) {
    }
}
