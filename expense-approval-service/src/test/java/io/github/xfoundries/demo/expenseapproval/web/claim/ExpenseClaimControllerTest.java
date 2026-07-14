package io.github.xfoundries.demo.expenseapproval.web.claim;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import io.github.xfoundries.demo.expenseapproval.application.command.CreateExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.command.SubmitExpenseClaimCommand;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimCommandDispatcher;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.ClaimSummary;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ClaimViews.PageResult;
import io.github.xfoundries.demo.expenseapproval.application.port.in.ExpenseClaimQueryUseCase;
import io.github.xfoundries.demo.expenseapproval.application.service.ClaimAccessDeniedException;
import io.github.xfoundries.demo.expenseapproval.boot.ExpenseApprovalApplication;
import io.github.xfoundries.demo.expenseapproval.domain.model.ClaimState;
import io.github.xfoundries.demo.expenseapproval.domain.model.ExpenseClaimId;
import io.github.xfoundries.demo.expenseapproval.support.PostgreSqlIntegrationTest;
import org.jfoundry.application.exception.NotFoundException;
import org.jfoundry.domain.exception.DomainRuleViolationException;
import org.jfoundry.domain.exception.DomainStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ExpenseApprovalApplication.class)
@AutoConfigureMockMvc
class ExpenseClaimControllerTest extends PostgreSqlIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ClaimCommandDispatcher commandDispatcher;
    @MockitoBean ExpenseClaimQueryUseCase queryUseCase;

    @Test
    void createsClaimAndReturnsLocation() throws Exception {
        when(commandDispatcher.dispatch(any(CreateExpenseClaimCommand.class)))
                .thenReturn(ExpenseClaimId.of("claim-1"));

        mockMvc.perform(post("/api/claims")
                        .header("X-User-Id", "employee-1")
                        .header("X-User-Role", "EMPLOYEE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Customer visit\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/claims/claim-1"));

        verify(commandDispatcher).dispatch(any(CreateExpenseClaimCommand.class));
    }

    @Test
    void missingIdentityHeaderReturnsProblemDetail400() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .header("X-User-Role", "EMPLOYEE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Customer visit\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidItemInputReturnsProblemDetail400() throws Exception {
        mockMvc.perform(post("/api/claims/claim-1/items")
                        .header("X-User-Id", "employee-1")
                        .header("X-User-Role", "EMPLOYEE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expenseDate":"2026-07-10","category":"TRAVEL",
                                 "amount":0,"description":"Taxi"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void jfoundryDomainExceptionsBecome409And422() throws Exception {
        doThrow(new DomainStateException("Wrong state"))
                .when(commandDispatcher).dispatch(any(SubmitExpenseClaimCommand.class));

        mockMvc.perform(post("/api/claims/claim-1/submit")
                        .header("X-User-Id", "employee-1")
                        .header("X-User-Role", "EMPLOYEE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        doThrow(new DomainRuleViolationException("Rule rejected"))
                .when(commandDispatcher).dispatch(any(SubmitExpenseClaimCommand.class));
        mockMvc.perform(post("/api/claims/claim-1/submit")
                        .header("X-User-Id", "employee-1")
                        .header("X-User-Role", "EMPLOYEE"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void notFoundAndAccessDeniedBecome404And403() throws Exception {
        doThrow(new NotFoundException("Expense claim not found"))
                .doThrow(new ClaimAccessDeniedException("Access denied"))
                .when(queryUseCase).getDetail(any(), any());
        mockMvc.perform(get("/api/claims/missing")
                        .header("X-User-Id", "employee-1")
                        .header("X-User-Role", "EMPLOYEE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/claims/claim-2")
                        .header("X-User-Id", "employee-1")
                        .header("X-User-Role", "EMPLOYEE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void listsOwnedClaimsWithStablePageShape() throws Exception {
        when(queryUseCase.findMine(any(), eq(ClaimState.DRAFT), any()))
                .thenReturn(new PageResult<>(List.of(new ClaimSummary(
                        "claim-1", "employee-1", "Draft", ClaimState.DRAFT,
                        new BigDecimal("0.00"), Instant.parse("2026-07-12T08:00:00Z"))),
                        1, 0, 20));

        mockMvc.perform(get("/api/claims/mine")
                        .header("X-User-Id", "employee-1")
                        .header("X-User-Role", "EMPLOYEE")
                        .queryParam("state", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("claim-1"))
                .andExpect(jsonPath("$.total").value(1));
    }
}
