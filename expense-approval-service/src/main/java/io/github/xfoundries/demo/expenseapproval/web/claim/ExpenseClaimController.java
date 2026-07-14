package io.github.xfoundries.demo.expenseapproval.web.claim;

import java.net.URI;

import io.github.xfoundries.demo.expenseapproval.application.command.Actor;
import io.github.xfoundries.demo.expenseapproval.application.command.AddExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.ApproveExpenseClaimByFinanceCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.CreateExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.RejectExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.RemoveExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.ReopenExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.SubmitExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.UpdateExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.WithdrawExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommandDispatcher;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ExpenseClaimQueryUseCase;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.web.claim.request.ClaimRequests.CreateClaimRequest;
import io.github.xfoundries.demo.expenseapproval.web.claim.request.ClaimRequests.ExpenseItemRequest;
import io.github.xfoundries.demo.expenseapproval.web.claim.request.ClaimRequests.RejectClaimRequest;
import io.github.xfoundries.demo.expenseapproval.web.claim.response.ClaimResponses;
import io.github.xfoundries.demo.expenseapproval.web.claim.response.ClaimResponses.DetailResponse;
import io.github.xfoundries.demo.expenseapproval.web.claim.response.ClaimResponses.PageResponse;
import io.github.xfoundries.demo.expenseapproval.web.claim.response.ClaimResponses.SummaryResponse;
import jakarta.validation.Valid;
import org.jfoundry.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
@PrimaryAdapter
public class ExpenseClaimController {

    private final ClaimCommandDispatcher commandDispatcher;
    private final ExpenseClaimQueryUseCase queryUseCase;
    private final RequestActorResolver actorResolver;

    public ExpenseClaimController(
            ClaimCommandDispatcher commandDispatcher,
            ExpenseClaimQueryUseCase queryUseCase,
            RequestActorResolver actorResolver) {
        this.commandDispatcher = commandDispatcher;
        this.queryUseCase = queryUseCase;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateClaimRequest request) {
        ExpenseClaimId id = commandDispatcher.dispatch(
                new CreateExpenseClaimCommand(actor(userId, role), request.title()));
        return ResponseEntity.created(URI.create("/api/claims/" + id.value())).build();
    }

    @PostMapping("/{claimId}/items")
    public ResponseEntity<Void> addItem(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId,
            @Valid @RequestBody ExpenseItemRequest request) {
        commandDispatcher.dispatch(new AddExpenseItemCommand(
                actor(userId, role), ExpenseClaimId.of(claimId),
                request.expenseDate(), request.category(), request.amount(),
                request.description(), request.receiptReference()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{claimId}/items/{itemId}")
    public ResponseEntity<Void> updateItem(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId,
            @PathVariable String itemId,
            @Valid @RequestBody ExpenseItemRequest request) {
        commandDispatcher.dispatch(new UpdateExpenseItemCommand(
                actor(userId, role), ExpenseClaimId.of(claimId), ExpenseItemId.of(itemId),
                request.expenseDate(), request.category(), request.amount(),
                request.description(), request.receiptReference()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{claimId}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId,
            @PathVariable String itemId) {
        commandDispatcher.dispatch(new RemoveExpenseItemCommand(
                actor(userId, role), ExpenseClaimId.of(claimId), ExpenseItemId.of(itemId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/submit")
    public ResponseEntity<Void> submit(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        commandDispatcher.dispatch(new SubmitExpenseClaimCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/withdraw")
    public ResponseEntity<Void> withdraw(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        commandDispatcher.dispatch(new WithdrawExpenseClaimCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/reopen")
    public ResponseEntity<Void> reopen(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        commandDispatcher.dispatch(new ReopenExpenseClaimCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/manager-approval")
    public ResponseEntity<Void> approveByManager(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        commandDispatcher.dispatch(new ApproveExpenseClaimByManagerCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/finance-approval")
    public ResponseEntity<Void> approveByFinance(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        commandDispatcher.dispatch(new ApproveExpenseClaimByFinanceCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/rejection")
    public ResponseEntity<Void> reject(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId,
            @Valid @RequestBody RejectClaimRequest request) {
        commandDispatcher.dispatch(new RejectExpenseClaimCommand(
                actor(userId, role), ExpenseClaimId.of(claimId), request.reason()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    public PageResponse<SummaryResponse> findMine(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) ClaimState state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ClaimResponses.from(queryUseCase.findMine(
                actor(userId, role), state, new PageQuery(page, size)));
    }

    @GetMapping("/manager-queue")
    public PageResponse<SummaryResponse> findManagerQueue(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ClaimResponses.from(queryUseCase.findManagerQueue(
                actor(userId, role), new PageQuery(page, size)));
    }

    @GetMapping("/finance-queue")
    public PageResponse<SummaryResponse> findFinanceQueue(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ClaimResponses.from(queryUseCase.findFinanceQueue(
                actor(userId, role), new PageQuery(page, size)));
    }

    @GetMapping("/{claimId}")
    public DetailResponse getDetail(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        return ClaimResponses.from(queryUseCase.getDetail(
                actor(userId, role), ExpenseClaimId.of(claimId)));
    }

    private Actor actor(String userId, String role) {
        return actorResolver.resolve(userId, role);
    }

}
