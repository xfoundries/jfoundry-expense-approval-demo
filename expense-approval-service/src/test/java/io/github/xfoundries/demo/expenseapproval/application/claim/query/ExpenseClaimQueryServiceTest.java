package io.github.xfoundries.demo.expenseapproval.application.claim.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalActor;
import io.github.xfoundries.demo.expenseapproval.application.identity.ApprovalRole;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.ClaimDetail;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.view.ClaimViews.PageQuery;
import io.github.xfoundries.demo.expenseapproval.application.claim.query.port.out.ExpenseClaimViewPort;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.domain.model.UserId;
import org.jfoundry.application.exception.InvalidArgumentException;
import org.jfoundry.application.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseClaimQueryServiceTest {

    @Mock ExpenseClaimViewPort viewPort;
    private ExpenseClaimQueryService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseClaimQueryService(viewPort);
    }

    @Test
    void queueRequiresMatchingRoleBeforeQueryingPort() {
        ApprovalActor employee = new ApprovalActor(UserId.of("employee-1"), ApprovalRole.EMPLOYEE);

        assertThatThrownBy(() -> service.findManagerQueue(employee, new PageQuery(0, 20)))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("MANAGER");
        verify(viewPort, never()).findPendingManager(new PageQuery(0, 20));
    }

    @Test
    void pageSizeIsBounded() {
        ApprovalActor employee = new ApprovalActor(UserId.of("employee-1"), ApprovalRole.EMPLOYEE);

        assertThatThrownBy(() -> service.findMine(employee, null, new PageQuery(0, 101)))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void missingDetailBecomesNotFound() {
        ExpenseClaimId id = ExpenseClaimId.of("missing");
        ApprovalActor employee = new ApprovalActor(UserId.of("employee-1"), ApprovalRole.EMPLOYEE);
        when(viewPort.findDetail(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(employee, id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void employeeCannotViewAnotherClaimantsDetail() {
        ExpenseClaimId id = ExpenseClaimId.of("claim-1");
        ApprovalActor employee = new ApprovalActor(UserId.of("employee-1"), ApprovalRole.EMPLOYEE);
        when(viewPort.findDetail(id)).thenReturn(Optional.of(detail("employee-2")));

        assertThatThrownBy(() -> service.getDetail(employee, id))
                .isInstanceOf(ClaimAccessDeniedException.class);
    }

    private static ClaimDetail detail(String claimantId) {
        Instant now = Instant.parse("2026-07-12T08:00:00Z");
        return new ClaimDetail(
                "claim-1", claimantId, "Claim", ClaimState.DRAFT,
                new BigDecimal("0.00"), now, now, null, null, List.of(), List.of());
    }
}
