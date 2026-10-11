package com.finsphere.frontend.client;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.auth.LoginRequest;
import com.finsphere.common.dto.auth.LoginResponse;
import com.finsphere.common.dto.auth.RegistrationRequest;
import com.finsphere.common.dto.auth.UserProfileResponse;
import com.finsphere.common.dto.chit.AuctionRequest;
import com.finsphere.common.dto.chit.AuctionResultResponse;
import com.finsphere.common.dto.chit.ChitCycleResponse;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.ChitPlanDetailResponse;
import com.finsphere.common.dto.chit.ChitPlanRequest;
import com.finsphere.common.dto.chit.ChitPlanResponse;
import com.finsphere.common.dto.chit.EnrollmentRequest;
import com.finsphere.common.dto.chit.MemberResponse;
import com.finsphere.common.dto.finance.LeaveType;
import com.finsphere.common.dto.finance.LoanRequest;
import com.finsphere.common.dto.finance.LoanResponse;
import com.finsphere.common.dto.finance.PaymentMode;
import com.finsphere.common.dto.finance.TransactionRequest;
import com.finsphere.common.dto.finance.TransactionResponse;
import com.finsphere.common.dto.finance.TransactionType;
import com.finsphere.common.security.AuthCookieService;
import com.finsphere.common.security.CookieProperties;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The HTTP service clients against a small HTTP server that answers like the services: every call must use the
 * method, path and query the services' controllers map, forward the browser's session cookie, correlation id,
 * address and user agent, and every DTO must survive the JSON round trip. Errors and outages must become
 * {@link BackendException} and {@link BackendUnavailableException}.
 */
class ClientContractTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final BackendCallContext CONTEXT = new BackendCallContext("fsn_auth_token=eyJ.test.sig",
            "cid-1234abcd", "203.0.113.9, 10.0.0.7", "Mozilla/5.0 (contract test)");

    private static final List<Seen> seen = new CopyOnWriteArrayList<>();

    private static final Map<String, Reply> replies = new ConcurrentHashMap<>();

    private static HttpServer server;

    private static String base;

    record Seen(String method, String path, String query, Map<String, String> headers, String body) {
    }

    record Reply(int status, String body, Map<String, String> headers, long delayMillis) {
    }

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", ClientContractTest::serve);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void authServiceCalls() throws Exception {
        AuthClient auth = client(AuthClient.class, "auth-service", base, Duration.ofSeconds(5));
        LoginResponse login = new LoginResponse(1L, "Ravi Kumar", "ADMIN");
        reply("POST /auth/login", 200, ApiResponse.ok("Login successful", login),
                Map.of("Set-Cookie", "fsn_auth_token=abc.def.ghi; Path=/; Max-Age=86400; HttpOnly; SameSite=Lax"));
        ResponseEntity<ApiResponse<LoginResponse>> entity = withContext(
                () -> auth.login(new LoginRequest("9840012345", "secret123")));
        Seen call = last();
        assertEquals("POST /auth/login", call.method + " " + call.path);
        assertTrue(call.headers.getOrDefault("content-type", "").startsWith("application/json"), call::toString);
        assertEquals("9840012345", json(call.body).path("identifier").stringValue());
        assertTrue(String.valueOf(entity.getHeaders().getFirst("Set-Cookie")).startsWith("fsn_auth_token=abc.def.ghi"),
                "the session cookie reaches the browser");
        assertEquals(login, entity.getBody().data());
        assertEquals("fsn_auth_token=eyJ.test.sig", call.headers.get("cookie"));
        assertEquals("cid-1234abcd", call.headers.get("x-correlation-id"));
        assertEquals("203.0.113.9, 10.0.0.7", call.headers.get("x-forwarded-for"));
        assertEquals("Mozilla/5.0 (contract test)", call.headers.get("user-agent"));

        UserProfileResponse profile = new UserProfileResponse(9L, "முருகன் செல்வம்", "9790012345", null, "S/o Selvam",
                "T. Nagar, Chennai", "CUSTOMER", true, Instant.parse("2026-10-09T13:30:00Z"));
        reply("POST /auth/register", 201, ApiResponse.created("Account successfully created", profile), Map.of());
        UserProfileResponse created = withContext(() -> BackendResults.data(auth.register(new RegistrationRequest(
                "முருகன் செல்வம்", "9790012345", null, "Murugan123", "T. Nagar, Chennai", "S/o Selvam", "CUSTOMER"))));
        assertEquals("/auth/register", last().path);
        assertEquals(profile, created, "Tamil names and instants survive the round trip");

        reply("POST /auth/logout", 200, ApiResponse.ok("Logged out successfully", null),
                Map.of("Set-Cookie", "fsn_auth_token=; Path=/; Max-Age=0"));
        ResponseEntity<ApiResponse<Void>> out = withContext(auth::logout);
        assertEquals("/auth/logout", last().path);
        assertTrue(String.valueOf(out.getHeaders().getFirst("Set-Cookie")).contains("Max-Age=0"));

        reply("GET /auth/me", 200, ApiResponse.ok("Current user", profile), Map.of());
        assertEquals(profile, withContext(() -> BackendResults.data(auth.me())));
        assertEquals("/auth/me", last().path);
    }

    @Test
    void schemeCalls() throws Exception {
        ChitClient chit = client(ChitClient.class, "chit-finance-service", base, Duration.ofSeconds(5));
        ChitPlanResponse plan = new ChitPlanResponse(1L, "Gold 1 lakh, January 2026", new BigDecimal("100000.00"), 20,
                new BigDecimal("5.00"), new BigDecimal("5000.00"), new BigDecimal("5000.00"),
                new BigDecimal("40000.00"), 20, 18, 6, 10, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 9, 1), true,
                Instant.parse("2026-09-30T06:00:00Z"));
        PageResponse<ChitPlanResponse> page = new PageResponse<>(List.of(plan), 0, 8, 1, 1, true, true);
        reply("GET /chits/plans/active", 200, ApiResponse.ok("Active schemes", page), Map.of());
        assertEquals(page, withContext(() -> BackendResults.data(chit.activePlans(0, 8))));
        assertEquals("/chits/plans/active?page=0&size=8", last().path + "?" + last().query);

        ChitCycleResponse cycle = new ChitCycleResponse(1001L, 1L, 1, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 11), new BigDecimal("38000.00"), new BigDecimal("1650.00"),
                new BigDecimal("3350.00"), 2L, true);
        ChitEnrollmentResponse slot = new ChitEnrollmentResponse(103L, 1L, plan.name(), 4L, "Priya Raman", 3, true,
                Instant.parse("2025-12-23T05:30:00Z"));
        ChitPlanDetailResponse detail = new ChitPlanDetailResponse(plan, List.of(cycle), List.of(slot),
                List.of(1, 2, 3));
        reply("GET /chits/plans/1", 200, ApiResponse.ok("Scheme details", detail), Map.of());
        assertEquals(detail, withContext(() -> BackendResults.data(chit.plan(1))));

        reply("POST /chits/plans/create", 201, ApiResponse.created("Scheme created successfully", plan), Map.of());
        withContext(() -> chit.createPlan(new ChitPlanRequest("Pongal 1 lakh", new BigDecimal("100000"), 20,
                new BigDecimal("5"), new BigDecimal("5000"), new BigDecimal("40000"), 20, LocalDate.of(2027, 1, 14),
                10)));
        JsonNode created = json(last().body);
        assertEquals("/chits/plans/create", last().path);
        assertEquals("2027-01-14", created.path("startDate").stringValue(), "dates travel as ISO dates");
        assertEquals(0, created.path("totalValue").decimalValue().compareTo(new BigDecimal("100000")));

        reply("POST /chits/enrollments/join", 201, ApiResponse.created("Enrolled in slot 3", slot), Map.of());
        withContext(() -> chit.enroll(new EnrollmentRequest(1L, 4L, 3)));
        assertEquals("/chits/enrollments/join", last().path);
        assertEquals(3, json(last().body).path("slotNumber").intValue());

        reply("GET /chits/enrollments/me", 200, ApiResponse.ok("Your enrolments", List.of(slot)), Map.of());
        assertEquals(List.of(slot), withContext(() -> BackendResults.data(chit.myEnrollments())));
        reply("GET /chits/enrollments/user/4", 200, ApiResponse.ok("Member enrolments", List.of(slot)), Map.of());
        assertEquals(List.of(slot), withContext(() -> BackendResults.data(chit.enrollmentsOf(4))));

        AuctionResultResponse result = new AuctionResultResponse(cycle, new BigDecimal("62000.00"),
                new BigDecimal("5000.00"), 1, 1);
        reply("POST /chits/cycles/auction", 200, ApiResponse.ok("Auction settled", result), Map.of());
        assertEquals(result, withContext(() -> BackendResults.data(chit.settleAuction(new AuctionRequest(1L, 7,
                new BigDecimal("15000"), 6L)))));
        assertEquals("/chits/cycles/auction", last().path);

        MemberResponse member = new MemberResponse(4L, "Priya Raman", "9876501234", "W/o Raman", "CUSTOMER");
        PageResponse<MemberResponse> members = new PageResponse<>(List.of(member), 0, 100, 1, 1, true, true);
        reply("GET /chits/members", 200, ApiResponse.ok("Members", members), Map.of());
        assertEquals(members, withContext(() -> BackendResults.data(chit.members(0, 100))));
        assertEquals("page=0&size=100", last().query);
    }

    @Test
    void loanAndPaymentCalls() throws Exception {
        FinanceClient finance = client(FinanceClient.class, "chit-finance-service", base, Duration.ofSeconds(5));
        LoanResponse loan = new LoanResponse(12L, 4L, "Priya Raman", new BigDecimal("10000.00"),
                new BigDecimal("1000.00"), new BigDecimal("9000.00"), new BigDecimal("100.00"), 100, LeaveType.SUNDAY,
                LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 12), LocalDate.of(2027, 2, 3), true,
                Instant.parse("2026-10-11T04:00:00Z"));
        reply("POST /finance/loans/disburse", 201, ApiResponse.created("Loan issued", loan), Map.of());
        assertEquals(loan, withContext(() -> BackendResults.data(finance.disburse(new LoanRequest(4L,
                new BigDecimal("10000"), new BigDecimal("1000"), 100, LocalDate.of(2026, 10, 12), LeaveType.SUNDAY)))));
        assertEquals("SUNDAY", json(last().body).path("leaveType").stringValue());

        reply("GET /finance/loans", 200, ApiResponse.ok("Loans", new PageResponse<>(List.of(loan), 0, 6, 1, 1, true,
                true)), Map.of());
        assertEquals(List.of(loan), withContext(() -> BackendResults.data(finance.loans(0, 6)).content()));
        assertEquals("page=0&size=6", last().query);
        reply("GET /finance/loans/me", 200, ApiResponse.ok("Your loans", List.of(loan)), Map.of());
        assertEquals(List.of(loan), withContext(() -> BackendResults.data(finance.myLoans())));
        reply("GET /finance/loans/user/4", 200, ApiResponse.ok("Loans", List.of(loan)), Map.of());
        assertEquals(List.of(loan), withContext(() -> BackendResults.data(finance.loansOf(4))));

        TransactionResponse payment = new TransactionResponse("6530af01", 4L, "Priya Raman",
                TransactionType.CHIT_PAYMENT, new BigDecimal("4650.50"), LocalDate.of(2026, 10, 9), PaymentMode.UPI,
                "UPI ref 6248 1139 0071", 1L, null, 1L, Instant.parse("2026-10-09T09:00:00Z"));
        reply("POST /finance/transactions/log", 201, ApiResponse.created("Payment recorded", payment), Map.of());
        assertEquals(payment, withContext(() -> BackendResults.data(finance.recordPayment(new TransactionRequest(4L,
                new BigDecimal("4650.50"), TransactionType.CHIT_PAYMENT, 1L, PaymentMode.UPI,
                "UPI ref 6248 1139 0071", null, LocalDate.of(2026, 10, 9))))));
        assertEquals(0, json(last().body).path("amount").decimalValue().compareTo(new BigDecimal("4650.50")),
                "paise survive");

        reply("GET /finance/transactions", 200, ApiResponse.ok("Payments", new PageResponse<>(List.of(payment), 0, 20,
                1, 1, true, true)), Map.of());
        assertEquals(List.of(payment), withContext(() -> BackendResults.data(finance.payments(0, 20)).content()));
        assertEquals("page=0&size=20", last().query);
        reply("GET /finance/transactions/me", 200, ApiResponse.ok("Your payments", new PageResponse<>(List.of(payment),
                0, 5, 1, 1, true, true)), Map.of());
        assertEquals(List.of(payment), withContext(() -> BackendResults.data(finance.myPayments(0, 5)).content()));
        assertEquals("page=0&size=5", last().query);
    }

    @Test
    void serviceErrorsBecomeBackendExceptions() {
        ChitClient chit = client(ChitClient.class, "chit-finance-service", base, Duration.ofSeconds(5));
        AuthClient auth = client(AuthClient.class, "auth-service", base, Duration.ofSeconds(5));

        Map<String, Object> conflict = new LinkedHashMap<>();
        conflict.put("success", false);
        conflict.put("status", 409);
        conflict.put("message", "That slot is already taken.");
        conflict.put("errors", Map.of("slotNumber", "Slot 9 was just taken by another member"));
        reply("POST /chits/enrollments/join", 409, conflict, Map.of());
        BackendException taken = assertThrows(BackendException.class,
                () -> withContext(() -> chit.enroll(new EnrollmentRequest(1L, 4L, 9))));
        assertEquals(409, taken.getStatus());
        assertTrue(taken.isRejection());
        assertEquals("That slot is already taken.", taken.userMessage());
        assertEquals("Slot 9 was just taken by another member", taken.getFieldErrors().get("slotNumber"));

        reply("GET /chits/enrollments/me", 401, Map.of("success", false, "status", 401, "message",
                "Your session has ended. Sign in again."), Map.of());
        BackendException ended = assertThrows(BackendException.class, () -> withContext(chit::myEnrollments));
        assertEquals(401, ended.getStatus());
        assertFalse(ended.isRejection());

        reply("GET /chits/plans/1", 500, Map.of("success", false, "status", 500, "message",
                "could not execute statement [ERROR: relation \"x\" does not exist]"), Map.of());
        BackendException fault = assertThrows(BackendException.class, () -> withContext(() -> chit.plan(1)));
        assertFalse(fault.userMessage().contains("relation"), "internal messages are not shown to people");
        assertEquals("chit-finance-service", fault.getService(), "errors name the service that answered");

        reply("GET /chits/plans/3", 503, Map.of("success", false, "status", 503, "message",
                "A backing service is temporarily unavailable. Please try again shortly"), Map.of());
        BackendException busy = assertThrows(BackendException.class, () -> withContext(() -> chit.plan(3)));
        assertTrue(busy.isServiceTrouble(), "a 503 from a running service is reported as such");

        replies.put("GET /chits/plans/active", new Reply(502, "<html><body>Bad Gateway</body></html>",
                Map.of("Content-Type", "text/html"), 0));
        assertEquals(502, assertThrows(BackendException.class,
                () -> withContext(() -> chit.activePlans(0, 8))).getStatus(), "a non-JSON error page is handled");

        reply("GET /auth/me", 200, ApiResponse.ok("Current user", null), Map.of());
        assertEquals(502, assertThrows(BackendException.class,
                () -> withContext(() -> BackendResults.data(auth.me()))).getStatus(), "an empty answer is a fault");
    }

    @Test
    void servicesThatAreDownOrSlowAreReportedAsUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        ChitClient down = client(ChitClient.class, "chit-finance-service", "http://127.0.0.1:" + closedPort,
                Duration.ofSeconds(2));
        BackendUnavailableException refused = assertThrows(BackendUnavailableException.class,
                () -> withContext(() -> down.plan(1)));
        assertEquals("chit-finance-service", refused.getService());
        assertFalse(refused.outcomeUnknown(), "a refused connection never reached the service");

        replies.put("GET /chits/plans/2", new Reply(200, "{}", Map.of("Content-Type", "application/json"), 3000));
        ChitClient slow = client(ChitClient.class, "chit-finance-service", base, Duration.ofSeconds(1));
        long start = System.nanoTime();
        BackendUnavailableException late = assertThrows(BackendUnavailableException.class,
                () -> withContext(() -> slow.plan(2)));
        assertTrue(System.nanoTime() - start < Duration.ofMillis(2500).toNanos(), "the read timeout applies");
        assertTrue(late.outcomeUnknown(), "a request that timed out waiting for the answer may have been carried out");
    }

    // ------------------------------------------------------------------------------------------ plumbing

    /** Built the way BackendClientConfig and Spring Boot build the clients: JDK client on HTTP/1.1. */
    private static <T> T client(Class<T> type, String service, String baseUrl, Duration readTimeout) {
        HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(readTimeout);
        AuthCookieService cookies = new AuthCookieService(new CookieProperties("fsn_auth_token", true, "Lax", "/",
                null), Duration.ofHours(24));
        RestClient restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .requestInterceptor(new BackendRequestInterceptor(service, new BackendCallContextResolver(cookies)))
            .defaultStatusHandler(HttpStatusCode::isError, new BackendErrorDecoder(JSON, service))
            .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(type);
    }

    /** Runs a call as the front end does while serving a browser request. */
    private static <T> T withContext(ScopedValue.CallableOp<T, RuntimeException> call) {
        return ScopedValue.where(BackendCallContext.CURRENT, CONTEXT).call(call);
    }

    private static void serve(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getRawPath();
        Map<String, String> headers = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(), String.join(", ",
                values)));
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        seen.add(new Seen(method, path, exchange.getRequestURI().getRawQuery(), headers, body));
        Reply reply = replies.getOrDefault(method + " " + path, new Reply(404, "{\"success\":false,\"status\":404,"
                + "\"message\":\"No route " + path + "\"}", Map.of("Content-Type", "application/json"), 0));
        if (reply.delayMillis > 0) {
            try {
                Thread.sleep(reply.delayMillis);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] bytes = reply.body.getBytes(StandardCharsets.UTF_8);
        reply.headers.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        if (!reply.headers.containsKey("Content-Type")) {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
        }
        try {
            exchange.sendResponseHeaders(reply.status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        catch (IOException ex) {
            // the client gave up waiting (read timeout test)
        }
    }

    private static void reply(String route, int status, Object body, Map<String, String> headers) {
        replies.put(route, new Reply(status, JSON.writeValueAsString(body), headers, 0));
    }

    private static Seen last() {
        return seen.get(seen.size() - 1);
    }

    private static JsonNode json(String body) {
        return JSON.readTree(body);
    }
}
