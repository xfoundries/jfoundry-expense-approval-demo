package io.github.xfoundries.demo.expenseapproval.application.claim.command;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.jfoundry.architecture.cqrs.CommandHandler;
import org.jfoundry.architecture.hexagonal.PrimaryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimCommandUseCaseStructureTest {

    @Test
    void doesNotRetainFixedCommandDispatcherTypes() {
        assertThat(classExists("io.github.xfoundries.demo.expenseapproval.application.claim.command.DefaultClaimCommandDispatcher"))
                .isFalse();
        assertThat(classExists("io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in.ClaimCommandDispatcher"))
                .isFalse();
    }

    @ParameterizedTest
    @MethodSource("useCaseBindings")
    void eachCommandHandlerImplementsItsBusinessNamedPrimaryPort(UseCaseBinding binding) throws Exception {
        Optional<Class<?>> port = classForName(binding.portClassName());

        assertThat(port)
                .as("primary port %s", binding.portClassName())
                .isPresent();

        Class<?> useCase = port.orElseThrow();
        assertThat(useCase.isInterface()).isTrue();
        assertThat(useCase).hasAnnotation(PrimaryPort.class);
        assertThat(useCase.isAssignableFrom(binding.handlerType())).isTrue();

        Method useCaseMethod = useCase.getDeclaredMethod(binding.methodName(), binding.commandType());
        Method handlerMethod = binding.handlerType()
                .getDeclaredMethod(binding.methodName(), binding.commandType());
        assertThat(handlerMethod.isAnnotationPresent(CommandHandler.class)).isTrue();
        assertThat(handlerMethod.getReturnType()).isEqualTo(useCaseMethod.getReturnType());
    }

    private static Stream<UseCaseBinding> useCaseBindings() {
        return Stream.of(
                binding("CreateExpenseClaimUseCase", "create", CreateExpenseClaimCommand.class,
                        CreateExpenseClaimCommandHandler.class),
                binding("AddExpenseItemUseCase", "addItem", AddExpenseItemCommand.class,
                        AddExpenseItemCommandHandler.class),
                binding("UpdateExpenseItemUseCase", "updateItem", UpdateExpenseItemCommand.class,
                        UpdateExpenseItemCommandHandler.class),
                binding("RemoveExpenseItemUseCase", "removeItem", RemoveExpenseItemCommand.class,
                        RemoveExpenseItemCommandHandler.class),
                binding("SubmitExpenseClaimUseCase", "submit", SubmitExpenseClaimCommand.class,
                        SubmitExpenseClaimCommandHandler.class),
                binding("ApproveExpenseClaimByManagerUseCase", "approveByManager",
                        ApproveExpenseClaimByManagerCommand.class,
                        ApproveExpenseClaimByManagerCommandHandler.class),
                binding("ApproveExpenseClaimByFinanceUseCase", "approveByFinance",
                        ApproveExpenseClaimByFinanceCommand.class,
                        ApproveExpenseClaimByFinanceCommandHandler.class),
                binding("RejectExpenseClaimUseCase", "reject", RejectExpenseClaimCommand.class,
                        RejectExpenseClaimCommandHandler.class),
                binding("ReopenExpenseClaimUseCase", "reopen", ReopenExpenseClaimCommand.class,
                        ReopenExpenseClaimCommandHandler.class),
                binding("WithdrawExpenseClaimUseCase", "withdraw", WithdrawExpenseClaimCommand.class,
                        WithdrawExpenseClaimCommandHandler.class));
    }

    private static UseCaseBinding binding(
            String useCaseName, String methodName, Class<?> commandType, Class<?> handlerType) {
        return new UseCaseBinding(
                "io.github.xfoundries.demo.expenseapproval.application.claim.command.port.in." + useCaseName,
                methodName,
                commandType,
                handlerType);
    }

    private static boolean classExists(String className) {
        return classForName(className).isPresent();
    }

    private static Optional<Class<?>> classForName(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException exception) {
            return Optional.empty();
        }
    }

    private record UseCaseBinding(
            String portClassName, String methodName, Class<?> commandType, Class<?> handlerType) {
    }
}
