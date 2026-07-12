package io.github.xfoundries.demo.expenseapproval.acceptance;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.xfoundries.demo.expenseapproval.boot.ExpenseApprovalApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ExpenseApprovalApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:expense-acceptance;DB_CLOSE_DELAY=-1",
                "spring.sql.init.mode=always"
        })
class ExpenseApprovalAcceptanceTest {

    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper objectMapper;

    @Test
    void smallClaimCompletesAfterManagerApproval() throws Exception {
        String id = createAndSubmit("small", "employee-small", "2000.00");

        assertStatus(post(path(id, "manager-approval"), null, "manager-1", "MANAGER"),
                HttpStatus.NO_CONTENT);

        JsonNode detail = detail(id, "employee-small", "EMPLOYEE");
        assertThat(detail.path("state").asText()).isEqualTo("APPROVED");
        assertThat(detail.path("actions")).hasSize(2);
    }

    @Test
    void highValueClaimCompletesAfterManagerAndFinanceApproval() throws Exception {
        String id = createAndSubmit("high", "employee-high", "2000.01");

        assertStatus(post(path(id, "manager-approval"), null, "manager-high", "MANAGER"),
                HttpStatus.NO_CONTENT);
        assertThat(detail(id, "finance-high", "FINANCE").path("state").asText())
                .isEqualTo("PENDING_FINANCE");

        assertStatus(post(path(id, "finance-approval"), null, "finance-high", "FINANCE"),
                HttpStatus.NO_CONTENT);
        assertThat(detail(id, "employee-high", "EMPLOYEE").path("state").asText())
                .isEqualTo("APPROVED");
    }

    @Test
    void rejectedClaimCanBeReopenedEditedResubmittedAndWithdrawn() throws Exception {
        String user = "employee-reopen";
        String id = createAndSubmit("reopen", user, "100.00");
        assertStatus(post(path(id, "rejection"), Map.of("reason", "Wrong category"),
                "manager-reopen", "MANAGER"), HttpStatus.NO_CONTENT);
        assertStatus(post(path(id, "reopen"), null, user, "EMPLOYEE"), HttpStatus.NO_CONTENT);

        String itemId = detail(id, user, "EMPLOYEE").path("items").get(0).path("id").asText();
        Map<String, Object> update = item("120.00");
        assertStatus(exchange(path(id, "items/" + itemId), HttpMethod.PUT, update, user, "EMPLOYEE"),
                HttpStatus.NO_CONTENT);
        assertStatus(post(path(id, "submit"), null, user, "EMPLOYEE"), HttpStatus.NO_CONTENT);
        assertStatus(post(path(id, "withdraw"), null, user, "EMPLOYEE"), HttpStatus.NO_CONTENT);

        JsonNode detail = detail(id, user, "EMPLOYEE");
        assertThat(detail.path("state").asText()).isEqualTo("WITHDRAWN");
        assertThat(detail.path("total").decimalValue()).isEqualByComparingTo("120.00");
        assertThat(detail.path("actions").findValuesAsText("type"))
                .containsExactly("SUBMITTED", "REJECTED", "REOPENED", "SUBMITTED", "WITHDRAWN");
    }

    @Test
    void selfApprovalAndSkippedStageReturnBusinessProblems() {
        String user = "employee-invalid";
        String id = createAndSubmit("invalid", user, "2000.01");

        ResponseEntity<String> selfApproval = post(
                path(id, "manager-approval"), null, user, "MANAGER");
        assertStatus(selfApproval, HttpStatus.UNPROCESSABLE_ENTITY);

        ResponseEntity<String> skippedFinance = post(
                path(id, "finance-approval"), null, "finance-invalid", "FINANCE");
        assertStatus(skippedFinance, HttpStatus.CONFLICT);
    }

    private String createAndSubmit(String suffix, String user, String amount) {
        ResponseEntity<String> created = post(
                "/api/claims", Map.of("title", "Claim " + suffix), user, "EMPLOYEE");
        assertStatus(created, HttpStatus.CREATED);
        URI location = created.getHeaders().getLocation();
        assertThat(location).isNotNull();
        String id = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);

        assertStatus(post(path(id, "items"), item(amount), user, "EMPLOYEE"),
                HttpStatus.NO_CONTENT);
        assertStatus(post(path(id, "submit"), null, user, "EMPLOYEE"),
                HttpStatus.NO_CONTENT);
        return id;
    }

    private JsonNode detail(String id, String user, String role) throws Exception {
        ResponseEntity<String> response = exchange(
                "/api/claims/" + id, HttpMethod.GET, null, user, role);
        assertStatus(response, HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> post(
            String path, Object body, String user, String role) {
        return exchange(path, HttpMethod.POST, body, user, role);
    }

    private ResponseEntity<String> exchange(
            String path, HttpMethod method, Object body, String user, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", user);
        headers.set("X-User-Role", role);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private static Map<String, Object> item(String amount) {
        return Map.of(
                "expenseDate", "2026-07-10",
                "category", "TRAVEL",
                "amount", new BigDecimal(amount),
                "description", "Taxi",
                "receiptReference", "receipt-1");
    }

    private static String path(String id, String action) {
        return "/api/claims/" + id + "/" + action;
    }

    private static void assertStatus(ResponseEntity<?> response, HttpStatus status) {
        assertThat(response.getStatusCode()).isEqualTo(status);
    }
}
