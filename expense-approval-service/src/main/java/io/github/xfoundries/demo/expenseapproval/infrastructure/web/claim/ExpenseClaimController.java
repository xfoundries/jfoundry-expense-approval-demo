package io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim;

import java.net.URI;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.AddExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByFinanceCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ApproveExpenseClaimByManagerCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.CreateExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RejectExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.RemoveExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.ReopenExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.SubmitExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.UpdateExpenseItemCommand;
import io.github.xfoundries.demo.expenseapproval.application.claim.command.WithdrawExpenseClaimCommand;
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
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.ExpenseClaimQueries;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseItemId;
import io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim.request.ClaimRequests.CreateClaimRequest;
import io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim.request.ClaimRequests.ExpenseItemRequest;
import io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim.request.ClaimRequests.RejectClaimRequest;
import io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim.response.ClaimResponses;
import io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim.response.ClaimResponses.DetailResponse;
import io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim.response.ClaimResponses.PageResponse;
import io.github.xfoundries.demo.expenseapproval.infrastructure.web.claim.response.ClaimResponses.SummaryResponse;
import jakarta.validation.Valid;
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
public class ExpenseClaimController {

    private final CreateExpenseClaimCommandHandler createExpenseClaimHandler;
    private final AddExpenseItemCommandHandler addExpenseItemHandler;
    private final UpdateExpenseItemCommandHandler updateExpenseItemHandler;
    private final RemoveExpenseItemCommandHandler removeExpenseItemHandler;
    private final SubmitExpenseClaimCommandHandler submitExpenseClaimHandler;
    private final ApproveExpenseClaimByManagerCommandHandler approveByManagerHandler;
    private final ApproveExpenseClaimByFinanceCommandHandler approveByFinanceHandler;
    private final RejectExpenseClaimCommandHandler rejectExpenseClaimHandler;
    private final ReopenExpenseClaimCommandHandler reopenExpenseClaimHandler;
    private final WithdrawExpenseClaimCommandHandler withdrawExpenseClaimHandler;
    private final ExpenseClaimQueries expenseClaimQueries;
    private final RequestActorResolver actorResolver;

    public ExpenseClaimController(
            CreateExpenseClaimCommandHandler createExpenseClaimHandler,
            AddExpenseItemCommandHandler addExpenseItemHandler,
            UpdateExpenseItemCommandHandler updateExpenseItemHandler,
            RemoveExpenseItemCommandHandler removeExpenseItemHandler,
            SubmitExpenseClaimCommandHandler submitExpenseClaimHandler,
            ApproveExpenseClaimByManagerCommandHandler approveByManagerHandler,
            ApproveExpenseClaimByFinanceCommandHandler approveByFinanceHandler,
            RejectExpenseClaimCommandHandler rejectExpenseClaimHandler,
            ReopenExpenseClaimCommandHandler reopenExpenseClaimHandler,
            WithdrawExpenseClaimCommandHandler withdrawExpenseClaimHandler,
            ExpenseClaimQueries expenseClaimQueries,
            RequestActorResolver actorResolver) {
        this.createExpenseClaimHandler = createExpenseClaimHandler;
        this.addExpenseItemHandler = addExpenseItemHandler;
        this.updateExpenseItemHandler = updateExpenseItemHandler;
        this.removeExpenseItemHandler = removeExpenseItemHandler;
        this.submitExpenseClaimHandler = submitExpenseClaimHandler;
        this.approveByManagerHandler = approveByManagerHandler;
        this.approveByFinanceHandler = approveByFinanceHandler;
        this.rejectExpenseClaimHandler = rejectExpenseClaimHandler;
        this.reopenExpenseClaimHandler = reopenExpenseClaimHandler;
        this.withdrawExpenseClaimHandler = withdrawExpenseClaimHandler;
        this.expenseClaimQueries = expenseClaimQueries;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateClaimRequest request) {
        ExpenseClaimId id = createExpenseClaimHandler.create(
                new CreateExpenseClaimCommand(actor(userId, role), request.title()));
        return ResponseEntity.created(URI.create("/api/claims/" + id.value())).build();
    }

    @PostMapping("/{claimId}/items")
    public ResponseEntity<Void> addItem(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId,
            @Valid @RequestBody ExpenseItemRequest request) {
        addExpenseItemHandler.addItem(new AddExpenseItemCommand(
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
        updateExpenseItemHandler.updateItem(new UpdateExpenseItemCommand(
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
        removeExpenseItemHandler.removeItem(new RemoveExpenseItemCommand(
                actor(userId, role), ExpenseClaimId.of(claimId), ExpenseItemId.of(itemId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/submit")
    public ResponseEntity<Void> submit(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        submitExpenseClaimHandler.submit(new SubmitExpenseClaimCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/withdraw")
    public ResponseEntity<Void> withdraw(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        withdrawExpenseClaimHandler.withdraw(new WithdrawExpenseClaimCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/reopen")
    public ResponseEntity<Void> reopen(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        reopenExpenseClaimHandler.reopen(new ReopenExpenseClaimCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/manager-approval")
    public ResponseEntity<Void> approveByManager(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        approveByManagerHandler.approveByManager(new ApproveExpenseClaimByManagerCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/finance-approval")
    public ResponseEntity<Void> approveByFinance(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        approveByFinanceHandler.approveByFinance(new ApproveExpenseClaimByFinanceCommand(
                actor(userId, role), ExpenseClaimId.of(claimId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{claimId}/rejection")
    public ResponseEntity<Void> reject(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId,
            @Valid @RequestBody RejectClaimRequest request) {
        rejectExpenseClaimHandler.reject(new RejectExpenseClaimCommand(
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
        return ClaimResponses.from(expenseClaimQueries.findMine(
                actor(userId, role), state, new PageQuery(page, size)));
    }

    @GetMapping("/manager-queue")
    public PageResponse<SummaryResponse> findManagerQueue(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ClaimResponses.from(expenseClaimQueries.findManagerQueue(
                actor(userId, role), new PageQuery(page, size)));
    }

    @GetMapping("/finance-queue")
    public PageResponse<SummaryResponse> findFinanceQueue(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ClaimResponses.from(expenseClaimQueries.findFinanceQueue(
                actor(userId, role), new PageQuery(page, size)));
    }

    @GetMapping("/{claimId}")
    public DetailResponse getDetail(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String claimId) {
        return ClaimResponses.from(expenseClaimQueries.getDetail(
                actor(userId, role), ExpenseClaimId.of(claimId)));
    }

    private ApprovalActor actor(String userId, String role) {
        return actorResolver.resolve(userId, role);
    }

}
