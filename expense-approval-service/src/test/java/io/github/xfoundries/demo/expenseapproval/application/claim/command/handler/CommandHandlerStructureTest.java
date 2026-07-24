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
import org.jfoundry.architecture.cqrs.Command;
import org.jfoundry.architecture.cqrs.CommandHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class CommandHandlerStructureTest {

    @ParameterizedTest
    @MethodSource("handlerCases")
    void eachCommandHasOneBusinessActionHandler(HandlerCase handlerCase) throws Exception {
        assertThat(handlerCase.commandType()).hasAnnotation(Command.class);
        Method action = handlerCase.handlerType().getDeclaredMethod(
                handlerCase.actionMethod(), handlerCase.commandType());
        assertThat(action.isAnnotationPresent(CommandHandler.class)).isTrue();
    }

    private static Stream<HandlerCase> handlerCases() {
        return Stream.of(
                new HandlerCase(AddExpenseItemCommand.class, AddExpenseItemCommandHandler.class, "addItem"),
                new HandlerCase(UpdateExpenseItemCommand.class, UpdateExpenseItemCommandHandler.class, "updateItem"),
                new HandlerCase(RemoveExpenseItemCommand.class, RemoveExpenseItemCommandHandler.class, "removeItem"),
                new HandlerCase(SubmitExpenseClaimCommand.class, SubmitExpenseClaimCommandHandler.class, "submit"),
                new HandlerCase(ApproveExpenseClaimByManagerCommand.class,
                        ApproveExpenseClaimByManagerCommandHandler.class, "approveByManager"),
                new HandlerCase(ApproveExpenseClaimByFinanceCommand.class,
                        ApproveExpenseClaimByFinanceCommandHandler.class, "approveByFinance"),
                new HandlerCase(RejectExpenseClaimCommand.class, RejectExpenseClaimCommandHandler.class, "reject"),
                new HandlerCase(ReopenExpenseClaimCommand.class, ReopenExpenseClaimCommandHandler.class, "reopen"),
                new HandlerCase(WithdrawExpenseClaimCommand.class, WithdrawExpenseClaimCommandHandler.class, "withdraw"));
    }

    private record HandlerCase(
            Class<?> commandType, Class<?> handlerType, String actionMethod) {
    }
}
