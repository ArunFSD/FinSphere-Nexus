import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * End-to-end smoke test of a running FinSphere Nexus stack, through the gateway, as a browser and an API client
 * would use it. Needs only a JDK (25+):
 *
 * <pre>
 *   java scripts/SmokeTest.java                       # http://localhost:7080
 *   java scripts/SmokeTest.java http://my-host:7080
 * </pre>
 *
 * The first administrator is created on a fresh database; on an existing one set FSN_SMOKE_ADMIN_PHONE and
 * FSN_SMOKE_ADMIN_PASSWORD to an administrator that can sign in. Exits with 1 on the first failed check.
 */
public class SmokeTest {

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    private static String base;

    private static int passed;

    public static void main(String[] args) throws Exception {
        base = (args.length > 0) ? args[0].replaceAll("/+$", "") : "http://localhost:7080";
        String adminPhone = env("FSN_SMOKE_ADMIN_PHONE", "9000000001");
        String adminPassword = env("FSN_SMOKE_ADMIN_PASSWORD", "Admin12345");

        step("Gateway routes to the web front end and both services", () -> waitFor(Duration.ofMinutes(4),
                () -> get("/login", null).statusCode() == 200
                        // 401 rather than 503 shows the service is registered and answering
                        && get("/auth/me", null).statusCode() == 401
                        && get("/chits/plans/active", null).statusCode() == 401));

        step("Health and metrics endpoints are not public", () -> {
            expect(get("/actuator/health", null), 404);
            expect(get("/actuator/prometheus", null), 404);
        });

        step("First administrator exists", () -> {
            HttpResponse<String> response = postJson("/auth/register", null, """
                    {"fullName":"Smoke Test Admin","phoneNumber":"%s","password":"%s",
                     "address":"FinSphere office","role":"ADMIN"}""".formatted(adminPhone, adminPassword));
            // 201 created now, 403 another administrator exists, 409 this one exists already
            expect(response, 201, 403, 409);
        });

        String adminCookie = signIn(adminPhone, adminPassword);

        String customerPhone = "9" + (100000000 + ThreadLocalRandom.current().nextInt(899999999));
        long customerId = step("Administrator adds a member", () -> {
            HttpResponse<String> response = postJson("/auth/register", adminCookie, """
                    {"fullName":"Smoke Member","phoneNumber":"%s","password":"Member1234",
                     "address":"Mylapore, Chennai","careOf":"S/o Tester","role":"CUSTOMER"}"""
                .formatted(customerPhone));
            expect(response, 201);
            return number(data(response), "userId");
        });

        String planName = "Smoke scheme " + Instant.now().getEpochSecond();
        long planId = step("Administrator creates a scheme with its monthly ledger", () -> {
            HttpResponse<String> response = postJson("/chits/plans/create", adminCookie, """
                    {"name":"%s","totalValue":20000,"durationMonths":2,"commissionPercentage":5,
                     "monthlyInstallment":10000,"maxBidLimit":8000,"totalMembers":2,"startDate":"%s",
                     "paymentDuration":10}""".formatted(planName, LocalDate.now()));
            expect(response, 201);
            return number(data(response), "id");
        });

        step("The new member reaches the scheme service through Kafka", () -> waitFor(Duration.ofSeconds(60), () -> {
            HttpResponse<String> response = get("/chits/members?size=100&sort=userId,desc", adminCookie);
            return response.statusCode() == 200 && list(data(response), "content").stream()
                .anyMatch((member) -> number(member, "userId") == customerId);
        }));

        step("Member is booked into slot 1", () -> expect(postJson("/chits/enrollments/join", adminCookie, """
                {"planId":%d,"userId":%d,"slotNumber":1}""".formatted(planId, customerId)), 201));

        step("Booking the same slot again is refused", () -> expect(postJson("/chits/enrollments/join", adminCookie,
                """
                {"planId":%d,"userId":%d,"slotNumber":1}""".formatted(planId, customerId)), 409));

        step("Month 1 auction is settled", () -> {
            HttpResponse<String> response = postJson("/chits/cycles/auction", adminCookie, """
                    {"planId":%d,"monthlyCount":1,"bidAmount":3000,"winnerUserId":%d}""".formatted(planId, customerId));
            expect(response, 200);
            Map<String, Object> result = data(response);
            check(decimal(result, "prizeMoney") == 17000.0, "prize money should be 17000, got " + result);
        });

        step("A payment is recorded", () -> expect(postJson("/finance/transactions/log", adminCookie, """
                {"userId":%d,"amount":9500,"transactionType":"CHIT_PAYMENT","targetId":%d,"paymentMode":"CASH",
                 "receivedByInfo":"Smoke test","paymentDate":"%s"}""".formatted(customerId, planId, LocalDate.now())),
                201));

        step("A loan is disbursed", () -> expect(postJson("/finance/loans/disburse", adminCookie, """
                {"userId":%d,"principalAmount":10000,"interestDeduction":1000,"tenureDays":100,
                 "startDate":"%s","leaveType":"SUNDAY"}""".formatted(customerId, LocalDate.now().plusDays(1))), 201));

        step("Dashboard and scheme pages render for the administrator", () -> {
            HttpResponse<String> dashboard = get("/dashboard", adminCookie);
            expect(dashboard, 200);
            check(dashboard.body().contains("Running schemes"), "dashboard does not list running schemes");
            HttpResponse<String> scheme = get("/schemes/" + planId, adminCookie);
            expect(scheme, 200);
            check(scheme.body().contains(planName), "scheme page does not show the scheme name");
        });

        step("The member signs in through the web form (with CSRF protection)", () -> {
            HttpResponse<String> page = get("/login", null);
            expect(page, 200);
            String csrfCookie = cookie(page, "XSRF-TOKEN")
                .orElseThrow(() -> new AssertionError("no XSRF-TOKEN cookie"));
            String csrfField = match(page.body(), "name=\"_csrf\" value=\"([^\"]+)\"");
            Map<String, String> form = new LinkedHashMap<>();
            form.put("identifier", customerPhone);
            form.put("password", "Member1234");
            form.put("next", "/dashboard");
            form.put("_csrf", csrfField);
            HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", "XSRF-TOKEN=" + csrfCookie)
                .POST(HttpRequest.BodyPublishers.ofString(formBody(form)))
                .build(), HttpResponse.BodyHandlers.ofString());
            expect(response, 302);
            check(response.headers().firstValue("Location").orElse("").endsWith("/dashboard"),
                    "sign-in did not redirect to the dashboard");
            String session = cookie(response, "fsn_auth_token")
                .orElseThrow(() -> new AssertionError("no session cookie"));
            HttpResponse<String> dashboard = get("/dashboard", "fsn_auth_token=" + session);
            expect(dashboard, 200);
            check(dashboard.body().contains("Your slots"), "member dashboard does not show the member's slots");
        });

        step("Members cannot open administrator pages", () -> {
            String memberCookie = signIn(customerPhone, "Member1234");
            expect(get("/members", memberCookie), 403);
            expect(postJson("/chits/plans/create", memberCookie, "{}"), 403);
        });

        step("Signing out ends the session", () -> {
            HttpResponse<String> response = postJson("/auth/logout", adminCookie, "");
            expect(response, 200);
            check(cookie(response, "fsn_auth_token").map(String::isEmpty).orElse(false), "cookie was not cleared");
        });

        System.out.printf("%nAll %d checks passed against %s%n", passed, base);
    }

    // ------------------------------------------------------------------------------------------ steps

    @FunctionalInterface
    interface Check<T> {

        T run() throws Exception;
    }

    @FunctionalInterface
    interface Action {

        void run() throws Exception;
    }

    private static <T> T step(String name, Check<T> check) {
        try {
            T result = check.run();
            passed++;
            System.out.println("  ok    " + name);
            return result;
        }
        catch (Throwable ex) {
            System.out.println("  FAIL  " + name + "\n        " + ex.getMessage());
            System.exit(1);
            return null;
        }
    }

    private static void step(String name, Action action) {
        step(name, () -> {
            action.run();
            return null;
        });
    }

    private static String signIn(String identifier, String password) {
        return step("Sign in as " + identifier, () -> {
            HttpResponse<String> response = postJson("/auth/login", null, """
                    {"identifier":"%s","password":"%s"}""".formatted(identifier, password));
            expect(response, 200);
            return "fsn_auth_token=" + cookie(response, "fsn_auth_token")
                .orElseThrow(() -> new AssertionError("login did not set the session cookie"));
        });
    }

    private static void waitFor(Duration timeout, Check<Boolean> condition) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        Exception last = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                if (Boolean.TRUE.equals(condition.run())) {
                    return;
                }
            }
            catch (IOException ex) {
                last = ex;
            }
            Thread.sleep(2000);
        }
        throw new AssertionError("not ready after " + timeout.toSeconds() + "s"
                + ((last != null) ? " (" + last.getMessage() + ")" : ""));
    }

    // ------------------------------------------------------------------------------------------ HTTP

    private static HttpResponse<String> get(String path, String cookie) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return HTTP.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postJson(String path, String cookie, String json)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return HTTP.send(request.POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void expect(HttpResponse<String> response, int... statuses) {
        for (int status : statuses) {
            if (response.statusCode() == status) {
                return;
            }
        }
        String body = response.body();
        throw new AssertionError(response.request().method() + " " + response.request().uri().getPath() + " returned "
                + response.statusCode() + ": " + ((body.length() > 300) ? body.substring(0, 300) + "…" : body));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static Optional<String> cookie(HttpResponse<?> response, String name) {
        return response.headers()
            .allValues("Set-Cookie")
            .stream()
            .filter((header) -> header.startsWith(name + "="))
            .map((header) -> header.substring(name.length() + 1, header.contains(";") ? header.indexOf(';')
                    : header.length()))
            .findFirst();
    }

    private static String formBody(Map<String, String> fields) {
        List<String> pairs = new ArrayList<>();
        fields.forEach((key, value) -> pairs.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8)));
        return String.join("&", pairs);
    }

    private static String match(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (!matcher.find()) {
            throw new AssertionError("pattern not found: " + regex);
        }
        return matcher.group(1);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    // ------------------------------------------------------------------------------------------ minimal JSON

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(HttpResponse<String> response) {
        Object root = new Json(response.body()).value();
        Object data = ((Map<String, Object>) root).get("data");
        if (!(data instanceof Map)) {
            throw new AssertionError("response has no data object: " + response.body());
        }
        return (Map<String, Object>) data;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> object, String field) {
        Object value = object.get(field);
        if (!(value instanceof List)) {
            throw new AssertionError("no list '" + field + "' in " + object);
        }
        return (List<Map<String, Object>>) value;
    }

    private static long number(Map<String, Object> object, String field) {
        return ((Number) object.get(field)).longValue();
    }

    private static double decimal(Map<String, Object> object, String field) {
        return ((Number) object.get(field)).doubleValue();
    }

    /** Just enough JSON to read the API's answers. */
    private static final class Json {

        private final String text;

        private int index;

        Json(String text) {
            this.text = text;
        }

        Object value() {
            skipSpace();
            char c = this.text.charAt(this.index);
            if (c == '{') {
                return object();
            }
            if (c == '[') {
                return array();
            }
            if (c == '"') {
                return string();
            }
            if (this.text.startsWith("true", this.index)) {
                this.index += 4;
                return Boolean.TRUE;
            }
            if (this.text.startsWith("false", this.index)) {
                this.index += 5;
                return Boolean.FALSE;
            }
            if (this.text.startsWith("null", this.index)) {
                this.index += 4;
                return null;
            }
            int start = this.index;
            while (this.index < this.text.length() && "+-0123456789.eE".indexOf(this.text.charAt(this.index)) >= 0) {
                this.index++;
            }
            String number = this.text.substring(start, this.index);
            return (number.contains(".") || number.contains("e") || number.contains("E")) ? Double.valueOf(number)
                    : Long.valueOf(number);
        }

        private Map<String, Object> object() {
            Map<String, Object> object = new LinkedHashMap<>();
            this.index++;
            skipSpace();
            if (this.text.charAt(this.index) == '}') {
                this.index++;
                return object;
            }
            while (true) {
                skipSpace();
                String key = string();
                skipSpace();
                this.index++; // ':'
                object.put(key, value());
                skipSpace();
                char next = this.text.charAt(this.index++);
                if (next == '}') {
                    return object;
                }
            }
        }

        private List<Object> array() {
            List<Object> array = new ArrayList<>();
            this.index++;
            skipSpace();
            if (this.text.charAt(this.index) == ']') {
                this.index++;
                return array;
            }
            while (true) {
                array.add(value());
                skipSpace();
                char next = this.text.charAt(this.index++);
                if (next == ']') {
                    return array;
                }
            }
        }

        private String string() {
            StringBuilder value = new StringBuilder();
            this.index++;
            while (true) {
                char c = this.text.charAt(this.index++);
                if (c == '"') {
                    return value.toString();
                }
                if (c == '\\') {
                    char escaped = this.text.charAt(this.index++);
                    switch (escaped) {
                        case 'n' -> value.append('\n');
                        case 't' -> value.append('\t');
                        case 'r' -> value.append('\r');
                        case 'b' -> value.append('\b');
                        case 'f' -> value.append('\f');
                        case 'u' -> {
                            value.append((char) Integer.parseInt(this.text.substring(this.index, this.index + 4), 16));
                            this.index += 4;
                        }
                        default -> value.append(escaped);
                    }
                }
                else {
                    value.append(c);
                }
            }
        }

        private void skipSpace() {
            while (this.index < this.text.length() && Character.isWhitespace(this.text.charAt(this.index))) {
                this.index++;
            }
        }
    }
}
