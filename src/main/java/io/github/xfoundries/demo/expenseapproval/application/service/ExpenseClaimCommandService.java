package io.github.xfoundries.demo.expenseapproval.application.service;

import java.time.Clock;
import java.time.Instant;

import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.Actor;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.ActorRole;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.AddItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.ApproveCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.ClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.CreateClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.RejectClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.RemoveItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommands.UpdateItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ExpenseClaimCommandUseCase;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaim;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItem;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.domain.model.Money;
import io.github.xfoundries.demo.expenseapproval.domain.model.RejectionReason;
import io.github.xfoundries.demo.expenseapproval.domain.repository.ExpenseClaimRepository;
import org.jfoundry.application.ApplicationService;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;
import org.jfoundry.application.transaction.ApplicationTransactional;

@ApplicationService
public final class ExpenseClaimCommandService implements ExpenseClaimCommandUseCase {

    private final ExpenseClaimRepository repository;
    private final Clock clock;

    public ExpenseClaimCommandService(ExpenseClaimRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @ApplicationTransactional
    public ExpenseClaimId create(CreateClaimCommand command) {
        requireRole(command.actor(), ActorRole.EMPLOYEE);
        Instant now = clock.instant();
        ExpenseClaim claim = ExpenseClaim.draft(
                ExpenseClaimId.generate(), command.actor().userId(), command.title(), now);
        repository.save(claim);
        return claim.id();
    }

    @Override
    @ApplicationTransactional
    public ExpenseItemId addItem(AddItemCommand command) {
        requireRole(command.actor(), ActorRole.EMPLOYEE);
        ExpenseClaim claim = load(command.claimId());
        ExpenseItem item = new ExpenseItem(
                ExpenseItemId.generate(),
                command.expenseDate(),
                command.category(),
                Money.positiveCny(command.amount()),
                command.description(),
                command.receiptReference());
        claim.addItem(command.actor().userId(), item, clock.instant());
        repository.save(claim);
        return item.id();
    }

    @Override
    @ApplicationTransactional
    public void updateItem(UpdateItemCommand command) {
        requireRole(command.actor(), ActorRole.EMPLOYEE);
        ExpenseClaim claim = load(command.claimId());
        ExpenseItem item = new ExpenseItem(
                command.itemId(),
                command.expenseDate(),
                command.category(),
                Money.positiveCny(command.amount()),
                command.description(),
                command.receiptReference());
        claim.updateItem(command.actor().userId(), item, clock.instant());
        repository.save(claim);
    }

    @Override
    @ApplicationTransactional
    public void removeItem(RemoveItemCommand command) {
        requireRole(command.actor(), ActorRole.EMPLOYEE);
        ExpenseClaim claim = load(command.claimId());
        claim.removeItem(command.actor().userId(), command.itemId(), clock.instant());
        repository.save(claim);
    }

    @Override
    @ApplicationTransactional
    public void submit(ClaimCommand command) {
        requireRole(command.actor(), ActorRole.EMPLOYEE);
        ExpenseClaim claim = load(command.claimId());
        claim.submit(command.actor().userId(), clock.instant());
        repository.save(claim);
    }

    @Override
    @ApplicationTransactional
    public void approveByManager(ApproveCommand command) {
        requireRole(command.actor(), ActorRole.MANAGER);
        ExpenseClaim claim = load(command.claimId());
        claim.approveByManager(command.actor().userId(), clock.instant());
        repository.save(claim);
    }

    @Override
    @ApplicationTransactional
    public void approveByFinance(ApproveCommand command) {
        requireRole(command.actor(), ActorRole.FINANCE);
        ExpenseClaim claim = load(command.claimId());
        claim.approveByFinance(command.actor().userId(), clock.instant());
        repository.save(claim);
    }

    @Override
    @ApplicationTransactional
    public void reject(RejectClaimCommand command) {
        ExpenseClaim claim = load(command.claimId());
        ActorRole requiredRole = switch (claim.state()) {
            case PENDING_MANAGER -> ActorRole.MANAGER;
            case PENDING_FINANCE -> ActorRole.FINANCE;
            default -> throw new InvalidArgumentException("Expense claim is not waiting for approval");
        };
        requireRole(command.actor(), requiredRole);
        claim.reject(
                command.actor().userId(), RejectionReason.of(command.reason()), clock.instant());
        repository.save(claim);
    }

    @Override
    @ApplicationTransactional
    public void reopen(ClaimCommand command) {
        requireRole(command.actor(), ActorRole.EMPLOYEE);
        ExpenseClaim claim = load(command.claimId());
        claim.reopen(command.actor().userId(), clock.instant());
        repository.save(claim);
    }

    @Override
    @ApplicationTransactional
    public void withdraw(ClaimCommand command) {
        requireRole(command.actor(), ActorRole.EMPLOYEE);
        ExpenseClaim claim = load(command.claimId());
        claim.withdraw(command.actor().userId(), clock.instant());
        repository.save(claim);
    }

    private ExpenseClaim load(ExpenseClaimId id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Expense claim not found: " + id.value()));
    }

    private static void requireRole(Actor actor, ActorRole requiredRole) {
        if (actor.role() != requiredRole) {
            throw new InvalidArgumentException("This action requires role " + requiredRole);
        }
    }
}
