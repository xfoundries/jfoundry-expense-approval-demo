package io.github.xfoundries.demo.e2e;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.xfoundries.demo.expenseapproval.boot.ExpenseApprovalApplication;
import io.github.xfoundries.demo.payment.boot.PaymentProcessorSimulatorApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExpenseApprovalMessagingEndToEndTest {

    private static final String EMPLOYEE_ID = "employee-payment-success";

    @Container
    static final PostgreSQLContainer<?> EXPENSE_DATABASE =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final PostgreSQLContainer<?> PAYMENT_DATABASE =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka-native:3.9.1");

    private final HttpClient http = HttpClient.newHttpClient();

    private ConfigurableApplicationContext paymentContext;
    private ServletWebServerApplicationContext expenseContext;
    private ObjectMapper objectMapper;
    private URI expenseBaseUri;

    @BeforeAll
    void startApplications() {
        paymentContext = new SpringApplicationBuilder(PaymentProcessorSimulatorApplication.class)
                .web(WebApplicationType.NONE)
                .run(asArguments(paymentProperties()));
        expenseContext = (ServletWebServerApplicationContext) new SpringApplicationBuilder(
                ExpenseApprovalApplication.class)
                .run(asArguments(expenseProperties()));
        objectMapper = expenseContext.getBean(ObjectMapper.class);
        expenseBaseUri = URI.create("http://localhost:" + expenseContext.getWebServer().getPort());
    }

    @AfterAll
    void stopApplications() {
        if (expenseContext != null) {
            expenseContext.close();
        }
        if (paymentContext != null) {
            paymentContext.close();
        }
    }

    @Test
    void approvedClaimIsEventuallyProjectedAsPaid() throws Exception {
        String claimId = createAndApproveClaim(EMPLOYEE_ID, new BigDecimal("1000.00"));

        JsonNode detail = awaitPaymentStatus(claimId, EMPLOYEE_ID, "PAID");

        assertThat(detail.path("state").asText()).isEqualTo("APPROVED");
        assertThat(detail.path("payment").path("paymentReference").asText())
                .isEqualTo("PAY-" + claimId);
    }

    @Test
    void paymentAboveSinglePaymentLimitIsEventuallyProjectedAsFailed() throws Exception {
        String employeeId = "employee-payment-failure";
        String claimId = createAndApproveClaim(employeeId, new BigDecimal("8000.01"));

        JsonNode detail = awaitPaymentStatus(claimId, employeeId, "FAILED");

        assertThat(detail.path("state").asText()).isEqualTo("APPROVED");
        assertThat(detail.path("payment").path("failureCode").asText())
                .isEqualTo("SINGLE_PAYMENT_LIMIT_EXCEEDED");
        assertThat(detail.path("payment").path("paymentReference").isNull()).isTrue();
    }

    @Test
    void duplicateApprovalDeliveryIsHandledOnceByPaymentInbox() throws Exception {
        String employeeId = "employee-duplicate-delivery";
        String claimId = createAndApproveClaim(employeeId, new BigDecimal("1200.00"));
        awaitPaymentStatus(claimId, employeeId, "PAID");

        String eventId = approvalEventId(claimId);
        assertThat(paymentInboxCount(eventId)).isEqualTo(1);
        assertThat(paymentResultOutboxCount(claimId)).isEqualTo(1);

        requeueApprovalEvent(eventId);
        awaitCondition("approval Outbox message to be republished", () ->
                "PUBLISHED".equals(expenseOutboxStatus(eventId)));
        Thread.sleep(500);

        assertThat(paymentInboxCount(eventId)).isEqualTo(1);
        assertThat(paymentResultOutboxCount(claimId)).isEqualTo(1);
    }

    @Test
    void concurrentFinalApprovalsCannotExceedMonthlyLimit() throws Exception {
        String employeeId = "employee-concurrent-limit";
        String firstClaim = createAwaitingFinanceApproval(employeeId, new BigDecimal("6000.00"));
        String secondClaim = createAwaitingFinanceApproval(employeeId, new BigDecimal("6000.00"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<HttpResponse<String>> first = executor.submit(() ->
                    approveByFinanceWhenReleased(firstClaim, ready, start));
            Future<HttpResponse<String>> second = executor.submit(() ->
                    approveByFinanceWhenReleased(secondClaim, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = List.of(
                    first.get(10, TimeUnit.SECONDS).statusCode(),
                    second.get(10, TimeUnit.SECONDS).statusCode());
            assertThat(statuses).containsExactlyInAnyOrder(204, 422);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        List<String> states = List.of(
                claimDetail(firstClaim, employeeId).path("state").asText(),
                claimDetail(secondClaim, employeeId).path("state").asText());
        assertThat(states).containsExactlyInAnyOrder("APPROVED", "PENDING_FINANCE");
        assertThat(approvalOutboxCount(firstClaim) + approvalOutboxCount(secondClaim)).isEqualTo(1);
    }

    @Test
    void rejectedOverLimitApprovalDoesNotWriteOutbox() throws Exception {
        String employeeId = "employee-sequential-limit";
        createAndApproveClaim(employeeId, new BigDecimal("7000.00"));
        String overLimitClaim = createAwaitingFinanceApproval(
                employeeId, new BigDecimal("4000.00"));
        assertThat(approvalOutboxCount(overLimitClaim)).isZero();

        HttpResponse<String> response = send(
                "POST", "/api/claims/" + overLimitClaim + "/finance-approval",
                "finance-1", "FINANCE", null);

        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(422);
        assertThat(claimDetail(overLimitClaim, employeeId).path("state").asText())
                .isEqualTo("PENDING_FINANCE");
        assertThat(approvalOutboxCount(overLimitClaim)).isZero();
    }

    private String createAndApproveClaim(String employeeId, BigDecimal amount) throws Exception {
        String claimId = createReadyForManagerApproval(employeeId, amount);
        assertStatus(send("POST", "/api/claims/" + claimId + "/manager-approval",
                "manager-1", "MANAGER", null), 204);
        if (amount.compareTo(new BigDecimal("2000.00")) > 0) {
            assertStatus(send("POST", "/api/claims/" + claimId + "/finance-approval",
                    "finance-1", "FINANCE", null), 204);
        }
        return claimId;
    }

    private String createAwaitingFinanceApproval(String employeeId, BigDecimal amount) throws Exception {
        String claimId = createReadyForManagerApproval(employeeId, amount);
        assertStatus(send("POST", "/api/claims/" + claimId + "/manager-approval",
                "manager-1", "MANAGER", null), 204);
        assertThat(claimDetail(claimId, employeeId).path("state").asText())
                .isEqualTo("PENDING_FINANCE");
        return claimId;
    }

    private String createReadyForManagerApproval(String employeeId, BigDecimal amount) throws Exception {
        HttpResponse<String> created = send("POST", "/api/claims", employeeId, "EMPLOYEE",
                "{\"title\":\"E2E expense\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        String claimId = URI.create(created.headers().firstValue("Location").orElseThrow())
                .getPath().replace("/api/claims/", "");

        String item = objectMapper.createObjectNode()
                .put("expenseDate", LocalDate.now().toString())
                .put("category", "TRAVEL")
                .put("amount", amount)
                .put("description", "E2E verification")
                .toString();
        assertStatus(send("POST", "/api/claims/" + claimId + "/items",
                employeeId, "EMPLOYEE", item), 204);
        assertStatus(send("POST", "/api/claims/" + claimId + "/submit",
                employeeId, "EMPLOYEE", null), 204);
        return claimId;
    }

    private HttpResponse<String> approveByFinanceWhenReleased(
            String claimId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent approvals");
        }
        return send("POST", "/api/claims/" + claimId + "/finance-approval",
                "finance-1", "FINANCE", null);
    }

    private JsonNode claimDetail(String claimId, String employeeId) throws Exception {
        HttpResponse<String> response = send(
                "GET", "/api/claims/" + claimId, employeeId, "EMPLOYEE", null);
        assertStatus(response, 200);
        return objectMapper.readTree(response.body());
    }

    private JsonNode awaitPaymentStatus(String claimId, String employeeId, String expected) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        JsonNode last = null;
        while (System.nanoTime() < deadline) {
            HttpResponse<String> response = send(
                    "GET", "/api/claims/" + claimId, employeeId, "EMPLOYEE", null);
            if (response.statusCode() == 200) {
                last = objectMapper.readTree(response.body());
                if (expected.equals(last.path("payment").path("status").asText())) {
                    return last;
                }
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Payment status did not become " + expected + "; last detail=" + last);
    }

    private HttpResponse<String> send(
            String method, String path, String userId, String role, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(expenseBaseUri.resolve(path))
                .timeout(Duration.ofSeconds(10))
                .header("X-User-Id", userId)
                .header("X-User-Role", role);
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertStatus(HttpResponse<String> response, int expected) {
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(expected);
    }

    private static String approvalEventId(String claimId) throws Exception {
        return queryString(
                EXPENSE_DATABASE,
                "select event_id from jfoundry_outbox_event "
                        + "where aggregate_id = ? and payload_type = 'ExpenseClaimApprovedV1'",
                claimId);
    }

    private static String expenseOutboxStatus(String eventId) throws Exception {
        return queryString(
                EXPENSE_DATABASE,
                "select status from jfoundry_outbox_event where event_id = ?",
                eventId);
    }

    private static int paymentInboxCount(String eventId) throws Exception {
        return queryInt(
                PAYMENT_DATABASE,
                "select count(*) from jfoundry_inbox_message where message_id = ?",
                eventId);
    }

    private static int paymentResultOutboxCount(String claimId) throws Exception {
        return queryInt(
                PAYMENT_DATABASE,
                "select count(*) from jfoundry_outbox_event where payload_key = ?",
                claimId);
    }

    private static int approvalOutboxCount(String claimId) throws Exception {
        return queryInt(
                EXPENSE_DATABASE,
                "select count(*) from jfoundry_outbox_event "
                        + "where aggregate_id = ? and payload_type = 'ExpenseClaimApprovedV1'",
                claimId);
    }

    private static void requeueApprovalEvent(String eventId) throws Exception {
        try (Connection connection = connection(EXPENSE_DATABASE);
             PreparedStatement statement = connection.prepareStatement("""
                     update jfoundry_outbox_event
                        set status = 'PENDING', retry_count = 0, error_message = null,
                            last_attempt_at = null, next_retry_at = null, updated_at = now()
                      where event_id = ?
                     """)) {
            statement.setString(1, eventId);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private static String queryString(
            PostgreSQLContainer<?> database, String sql, String argument) throws Exception {
        try (Connection connection = connection(database);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, argument);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getString(1);
            }
        }
    }

    private static int queryInt(
            PostgreSQLContainer<?> database, String sql, String argument) throws Exception {
        try (Connection connection = connection(database);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, argument);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getInt(1);
            }
        }
    }

    private static Connection connection(PostgreSQLContainer<?> database) throws Exception {
        return DriverManager.getConnection(
                database.getJdbcUrl(), database.getUsername(), database.getPassword());
    }

    private static void awaitCondition(String description, CheckedCondition condition) throws Exception {
        await(description)
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(100))
                .until(condition::matches);
    }

    private static Map<String, Object> paymentProperties() {
        return Map.ofEntries(
                Map.entry("spring.application.name", "payment-processor-simulator"),
                Map.entry("spring.datasource.url", PAYMENT_DATABASE.getJdbcUrl()),
                Map.entry("spring.datasource.username", PAYMENT_DATABASE.getUsername()),
                Map.entry("spring.datasource.password", PAYMENT_DATABASE.getPassword()),
                Map.entry("spring.flyway.locations", moduleMigrationLocation(
                        "payment-processor-simulator")),
                Map.entry("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers()),
                Map.entry("spring.kafka.consumer.auto-offset-reset", "earliest"),
                Map.entry("payment-simulator.single-payment-limit.amount", "8000.00"),
                Map.entry("payment-simulator.single-payment-limit.currency", "CNY"),
                Map.entry("payment-simulator.messaging.kafka-listener-enabled", "true"),
                Map.entry("jfoundry.outbox.dispatcher.mode", "scheduled"),
                Map.entry("jfoundry.outbox.dispatcher.interval-ms", "100"),
                Map.entry("spring.autoconfigure.exclude",
                        "org.redisson.spring.starter.RedissonAutoConfigurationV2"),
                Map.entry("spring.jmx.enabled", "false"));
    }

    private static Map<String, Object> expenseProperties() {
        return Map.ofEntries(
                Map.entry("spring.application.name", "expense-approval-service"),
                Map.entry("server.port", "0"),
                Map.entry("spring.datasource.url", EXPENSE_DATABASE.getJdbcUrl()),
                Map.entry("spring.datasource.username", EXPENSE_DATABASE.getUsername()),
                Map.entry("spring.datasource.password", EXPENSE_DATABASE.getPassword()),
                Map.entry("spring.flyway.locations", moduleMigrationLocation(
                        "expense-approval-service")),
                Map.entry("spring.data.redis.host", REDIS.getHost()),
                Map.entry("spring.data.redis.port", REDIS.getMappedPort(6379)),
                Map.entry("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers()),
                Map.entry("spring.kafka.consumer.auto-offset-reset", "earliest"),
                Map.entry("expense-approval.messaging.kafka-listener-enabled", "true"),
                Map.entry("jfoundry.outbox.dispatcher.mode", "scheduled"),
                Map.entry("jfoundry.outbox.dispatcher.interval-ms", "100"),
                Map.entry("spring.jmx.enabled", "false"));
    }

    private static String[] asArguments(Map<String, Object> properties) {
        return properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + '=' + entry.getValue())
                .toArray(String[]::new);
    }

    private static String moduleMigrationLocation(String module) {
        Path moduleRoot = Path.of(System.getProperty("user.dir"), "..", module)
                .toAbsolutePath()
                .normalize();
        return "filesystem:" + moduleRoot.resolve("src/main/resources/db/migration");
    }

    @FunctionalInterface
    private interface CheckedCondition {
        boolean matches() throws Exception;
    }
}
