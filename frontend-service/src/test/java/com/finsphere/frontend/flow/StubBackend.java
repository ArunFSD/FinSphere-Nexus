package com.finsphere.frontend.flow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

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
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.JwtService;
import com.finsphere.frontend.client.AuthClient;
import com.finsphere.frontend.client.BackendCallContext;
import com.finsphere.frontend.client.BackendCallContextResolver;
import com.finsphere.frontend.client.BackendException;
import com.finsphere.frontend.client.BackendUnavailableException;
import com.finsphere.frontend.client.ChitClient;
import com.finsphere.frontend.client.FinanceClient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Stands in for auth-service and chit-finance-service behind the three HTTP service clients (the HTTP contract itself
 * is covered by {@code ClientContractTest}). It behaves like the real services where the web app depends on it:
 * every call except sign-in and sign-up must carry the browser's session cookie (passed on through the call context,
 * also on the parallel section threads), administrator-only calls answer 403, a taken slot answers 409, invalid input
 * answers 400 with field errors, and any call can be made to fail on purpose.
 */
public final class StubBackend implements AuthClient, ChitClient, FinanceClient {

    static final String[] NAMES = { "Ravi Kumar", "Lakshmi Narayanan", "Senthil Murugan", "Priya Raman",
            "Karthik Subramanian", "Meena Sundaram", "Arul Prakash", "Divya Krishnan", "Ganesh Babu",
            "Kavitha Rajendran", "Mohan Das", "Revathi Selvam", "Suresh Pandian", "Anitha Gopal", "Vijay Anand",
            "Deepa Venkatesan", "Balaji Srinivasan", "Nirmala Devi" };

    /** Enough members that the pickers need three pages from the API. */
    static final int MEMBER_COUNT = 230;

    static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "9840012345", "ADMIN", "Ravi Kumar",
            "ravi@finsphere.in");

    static final AuthenticatedUser MEMBER = new AuthenticatedUser(4L, "9876501234", "CUSTOMER", "Priya Raman",
            null);

    static final String ADMIN_PASSWORD = "Admin12345";

    static final String MEMBER_PASSWORD = "Member1234";

    /** Calls that should fail: method name to the exception to throw. */
    final Map<String, RuntimeException> failures = new ConcurrentHashMap<>();

    /** Every call as "method cookie=<present|missing> thread=<virtual|platform>". */
    final List<String> calls = new CopyOnWriteArrayList<>();

    final List<Object> requests = new CopyOnWriteArrayList<>();

    private final Clock clock;

    private final JwtService jwtService;

    private final AuthCookieService cookieService;

    private BackendCallContextResolver resolver;

    private final List<MemberResponse> members = new CopyOnWriteArrayList<>();

    private final Map<Long, ChitPlanResponse> plans = new ConcurrentHashMap<>();

    private final List<ChitEnrollmentResponse> enrollments = new CopyOnWriteArrayList<>();

    private final Map<Long, List<ChitCycleResponse>> cycles = new ConcurrentHashMap<>();

    private final List<LoanResponse> loans = new CopyOnWriteArrayList<>();

    private final List<TransactionResponse> payments = new CopyOnWriteArrayList<>();

    private final AtomicLong ids = new AtomicLong(5000);

    StubBackend(Clock clock, JwtService jwtService, AuthCookieService cookieService) {
        this.clock = clock;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        seed();
    }

    void setResolver(BackendCallContextResolver resolver) {
        this.resolver = resolver;
    }

    // ------------------------------------------------------------------------------------------ auth-service

    @Override
    public ResponseEntity<ApiResponse<LoginResponse>> login(LoginRequest request) {
        record("login", false);
        AuthenticatedUser user = null;
        if (ADMIN.identifier().equals(request.identifier()) && ADMIN_PASSWORD.equals(request.password())) {
            user = ADMIN;
        }
        else if (MEMBER.identifier().equals(request.identifier()) && MEMBER_PASSWORD.equals(request.password())) {
            user = MEMBER;
        }
        if (user == null) {
            throw new BackendException(401, "The mobile number, e-mail or password is not correct.", Map.of());
        }
        String cookie = this.cookieService.cookieName() + "=" + this.jwtService.issue(user)
                + "; Path=/; Max-Age=86400; HttpOnly; SameSite=Lax";
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie)
            .body(ApiResponse.ok("Login successful", new LoginResponse(user.userId(), user.fullName(), user.role())));
    }

    @Override
    public ApiResponse<UserProfileResponse> register(RegistrationRequest request) {
        record("register", false);
        this.requests.add(request);
        Map<String, String> errors = new LinkedHashMap<>();
        if (request.fullName() == null || request.fullName().isBlank()) {
            errors.put("fullName", "Enter the full name");
        }
        if (request.phoneNumber() == null || !request.phoneNumber().matches("[6-9]\\d{9}")) {
            errors.put("phoneNumber", "Enter a 10-digit mobile number");
        }
        if (!errors.isEmpty()) {
            throw new BackendException(400, "Some details need changing. Check the highlighted fields.", errors);
        }
        if (this.members.stream().anyMatch((member) -> member.phoneNumber().equals(request.phoneNumber()))) {
            throw new BackendException(409, "Some details need changing. Check the highlighted fields.",
                    Map.of("phoneNumber", "This mobile number already has an account"));
        }
        long id = this.ids.incrementAndGet();
        this.members.add(new MemberResponse(id, request.fullName(), request.phoneNumber(), request.careOf(),
                request.role()));
        return ApiResponse.created("Account successfully created", new UserProfileResponse(id, request.fullName(),
                request.phoneNumber(), request.email(), request.careOf(), request.address(), request.role(), true,
                this.clock.instant()));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> logout() {
        record("logout", false);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, this.cookieService.cookieName() + "=; Path=/; Max-Age=0; HttpOnly")
            .body(ApiResponse.ok("Logged out successfully", null));
    }

    @Override
    public ApiResponse<UserProfileResponse> me() {
        AuthenticatedUser caller = caller("me");
        return ApiResponse.ok("Current user", new UserProfileResponse(caller.userId(), caller.fullName(),
                caller.identifier(), caller.email(), caller.isAdmin() ? null : "W/o Raman",
                "12, Kutchery Road, Mylapore", caller.role(), true, Instant.parse("2024-02-11T05:30:00Z")));
    }

    // ------------------------------------------------------------------------------------------ chits

    @Override
    public ApiResponse<PageResponse<ChitPlanResponse>> activePlans(int page, int size) {
        caller("activePlans");
        List<ChitPlanResponse> all = new ArrayList<>(this.plans.values());
        all.sort((a, b) -> Long.compare(a.id(), b.id()));
        return ApiResponse.ok("Active schemes", slice(all, page, size));
    }

    @Override
    public ApiResponse<ChitPlanDetailResponse> plan(long planId) {
        AuthenticatedUser caller = caller("plan");
        ChitPlanResponse plan = Optional.ofNullable(this.plans.get(planId))
            .orElseThrow(() -> new BackendException(404, "Scheme not found", Map.of()));
        List<ChitEnrollmentResponse> inPlan = this.enrollments.stream()
            .filter((enrollment) -> enrollment.planId().equals(planId) && enrollment.active())
            .toList();
        List<Integer> taken = inPlan.stream().map(ChitEnrollmentResponse::slotNumber).sorted().toList();
        List<ChitEnrollmentResponse> visible = caller.isAdmin() ? inPlan
                : inPlan.stream().filter((enrollment) -> enrollment.userId().equals(caller.userId())).toList();
        ChitPlanResponse current = withCounts(plan);
        return ApiResponse.ok("Scheme details", new ChitPlanDetailResponse(current, this.cycles.get(planId), visible,
                taken));
    }

    @Override
    public ApiResponse<ChitPlanResponse> createPlan(ChitPlanRequest request) {
        requireAdmin(caller("createPlan"));
        this.requests.add(request);
        Map<String, String> errors = new LinkedHashMap<>();
        if (request.name() == null || request.name().isBlank()) {
            errors.put("name", "Enter a scheme name");
        }
        if (request.totalValue() == null) {
            errors.put("totalValue", "Enter the chit value");
        }
        if (request.totalMembers() == null || request.totalMembers() < 2) {
            errors.put("totalMembers", "A scheme needs at least 2 members");
        }
        if (!errors.isEmpty()) {
            throw new BackendException(400, "Some scheme details need changing.", errors);
        }
        long id = this.ids.incrementAndGet();
        ChitPlanResponse plan = plan(id, request.name(), request.totalValue(), request.durationMonths(),
                request.commissionPercentage(), request.monthlyInstallment(), request.maxBidLimit(),
                request.totalMembers(), request.startDate());
        this.plans.put(id, plan);
        this.cycles.put(id, ledger(plan, 0, new long[0], new String[0]));
        return ApiResponse.created("Scheme created successfully", plan);
    }

    @Override
    public ApiResponse<ChitEnrollmentResponse> enroll(EnrollmentRequest request) {
        AuthenticatedUser caller = caller("enroll");
        this.requests.add(request);
        if (!caller.isAdmin() && !caller.userId().equals(request.userId())) {
            throw new BackendException(403, "You can only enrol yourself into a scheme", Map.of());
        }
        ChitPlanResponse plan = this.plans.get(request.planId());
        if (plan == null) {
            throw new BackendException(404, "Scheme not found", Map.of());
        }
        if (request.slotNumber() < 1 || request.slotNumber() > plan.totalMembers()) {
            throw new BackendException(400, "That slot is not part of this scheme.",
                    Map.of("slotNumber", "Choose a slot from 1 to " + plan.totalMembers()));
        }
        boolean taken = this.enrollments.stream()
            .anyMatch((e) -> e.planId().equals(request.planId()) && e.slotNumber().equals(request.slotNumber())
                    && e.active());
        if (taken) {
            throw new BackendException(409, "That slot is already taken.",
                    Map.of("slotNumber", "Slot " + request.slotNumber() + " was just taken by another member"));
        }
        ChitEnrollmentResponse enrollment = new ChitEnrollmentResponse(this.ids.incrementAndGet(), plan.id(),
                plan.name(), request.userId(), name(request.userId()), request.slotNumber(), true,
                this.clock.instant());
        this.enrollments.add(enrollment);
        return ApiResponse.created("Enrolled in slot " + request.slotNumber(), enrollment);
    }

    @Override
    public ApiResponse<List<ChitEnrollmentResponse>> myEnrollments() {
        AuthenticatedUser caller = caller("myEnrollments");
        return ApiResponse.ok("Your enrolments", enrollmentsOfUser(caller.userId()));
    }

    @Override
    public ApiResponse<List<ChitEnrollmentResponse>> enrollmentsOf(long userId) {
        AuthenticatedUser caller = caller("enrollmentsOf");
        if (!caller.isAdmin() && caller.userId() != userId) {
            throw new BackendException(403, "Access denied", Map.of());
        }
        return ApiResponse.ok("Member enrolments", enrollmentsOfUser(userId));
    }

    @Override
    public ApiResponse<AuctionResultResponse> settleAuction(AuctionRequest request) {
        requireAdmin(caller("settleAuction"));
        this.requests.add(request);
        ChitPlanResponse plan = this.plans.get(request.planId());
        List<ChitCycleResponse> ledger = this.cycles.get(request.planId());
        if (plan == null || ledger == null) {
            throw new BackendException(404, "Scheme not found", Map.of());
        }
        if (request.bidAmount().compareTo(plan.maxBidLimit()) > 0) {
            throw new BackendException(400, "The bid is above the scheme's limit.",
                    Map.of("bidAmount", "Bids in this scheme can be at most ₹" + plan.maxBidLimit().toPlainString()
                            + "."));
        }
        int index = request.monthlyCount() - 1;
        ChitCycleResponse cycle = ledger.get(index);
        if (cycle.closed()) {
            throw new BackendException(409, "This month is already settled.",
                    Map.of("monthlyCount", "This month is already settled"));
        }
        BigDecimal commission = plan.commissionAmount();
        BigDecimal dividend = request.bidAmount().subtract(commission).max(BigDecimal.ZERO)
            .divide(BigDecimal.valueOf(plan.totalMembers()), 2, RoundingMode.HALF_UP);
        ChitCycleResponse settled = new ChitCycleResponse(cycle.id(), cycle.planId(), cycle.monthlyCount(),
                cycle.paymentWindowStart(), cycle.paymentWindowDeadline(), request.bidAmount(), dividend,
                plan.monthlyInstallment().subtract(dividend), request.winnerUserId(), true);
        ledger.set(index, settled);
        return ApiResponse.ok("Auction settled. The winner receives ₹"
                + plan.totalValue().subtract(request.bidAmount()).toPlainString() + ".", new AuctionResultResponse(
                        settled, plan.totalValue().subtract(request.bidAmount()), commission, 1, 1));
    }

    @Override
    public ApiResponse<PageResponse<MemberResponse>> members(int page, int size) {
        requireAdmin(caller("members"));
        if (size > 100) {
            throw new BackendException(400, "Page size can be at most 100", Map.of());
        }
        return ApiResponse.ok("Members", slice(this.members, page, size));
    }

    // ------------------------------------------------------------------------------------------ finance

    @Override
    public ApiResponse<LoanResponse> disburse(LoanRequest request) {
        requireAdmin(caller("disburse"));
        this.requests.add(request);
        if (request.principalAmount() == null || request.userId() == null) {
            Map<String, String> errors = new LinkedHashMap<>();
            if (request.userId() == null) {
                errors.put("userId", "Choose a member");
            }
            if (request.principalAmount() == null) {
                errors.put("principalAmount", "Enter the loan amount");
            }
            throw new BackendException(400, "Some loan details need changing.", errors);
        }
        LoanResponse loan = loan(this.ids.incrementAndGet(), request.userId(), request.principalAmount(),
                request.interestDeduction(), request.tenureDays(), request.leaveType(), request.startDate(), true);
        this.loans.add(0, loan);
        return ApiResponse.created("Loan issued. Collection ends on " + loan.endDate(), loan);
    }

    @Override
    public ApiResponse<PageResponse<LoanResponse>> loans(int page, int size) {
        requireAdmin(caller("loans"));
        return ApiResponse.ok("Loans", slice(this.loans, page, size));
    }

    @Override
    public ApiResponse<List<LoanResponse>> myLoans() {
        AuthenticatedUser caller = caller("myLoans");
        return ApiResponse.ok("Your loans", loansOfUser(caller.userId()));
    }

    @Override
    public ApiResponse<List<LoanResponse>> loansOf(long userId) {
        AuthenticatedUser caller = caller("loansOf");
        if (!caller.isAdmin() && caller.userId() != userId) {
            throw new BackendException(403, "Access denied", Map.of());
        }
        return ApiResponse.ok("Loans", loansOfUser(userId));
    }

    @Override
    public ApiResponse<TransactionResponse> recordPayment(TransactionRequest request) {
        requireAdmin(caller("recordPayment"));
        this.requests.add(request);
        boolean matches = (request.transactionType() == TransactionType.CHIT_PAYMENT)
                ? enrollmentsOfUser(request.userId()).stream().anyMatch((e) -> e.planId().equals(request.targetId()))
                : loansOfUser(request.userId()).stream().anyMatch((l) -> l.id().equals(request.targetId()));
        if (!matches) {
            throw new BackendException(400, "This payment does not match the member's schemes or loans.",
                    Map.of("targetId", "The member is not enrolled in scheme " + request.targetId()));
        }
        TransactionResponse payment = new TransactionResponse("pay" + this.ids.incrementAndGet(), request.userId(),
                name(request.userId()), request.transactionType(), request.amount(), request.paymentDate(),
                request.paymentMode(), request.receivedByInfo(), request.targetId(), request.remarks(), ADMIN.userId(),
                this.clock.instant());
        this.payments.add(0, payment);
        return ApiResponse.created("Payment recorded in the log book", payment);
    }

    @Override
    public ApiResponse<PageResponse<TransactionResponse>> payments(int page, int size) {
        requireAdmin(caller("payments"));
        return ApiResponse.ok("Payments", slice(this.payments, page, size));
    }

    @Override
    public ApiResponse<PageResponse<TransactionResponse>> myPayments(int page, int size) {
        AuthenticatedUser caller = caller("myPayments");
        return ApiResponse.ok("Your payments", slice(this.payments.stream()
            .filter((payment) -> payment.userId().equals(caller.userId()))
            .toList(), page, size));
    }

    // ------------------------------------------------------------------------------------------ plumbing

    /** Records the call, applies an injected failure, and resolves the caller from the forwarded cookie. */
    private AuthenticatedUser caller(String method) {
        BackendCallContext context = record(method, true);
        String header = (context != null) ? context.cookieHeader() : null;
        String prefix = this.cookieService.cookieName() + "=";
        Optional<AuthenticatedUser> user = (header != null && header.startsWith(prefix))
                ? this.jwtService.parse(header.substring(prefix.length())) : Optional.empty();
        return user.orElseThrow(() -> new BackendException(401, "Authentication required", Map.of()));
    }

    private BackendCallContext record(String method, boolean needsSession) {
        BackendCallContext context = (this.resolver != null) ? this.resolver.current() : null;
        boolean cookie = context != null && context.cookieHeader() != null;
        this.calls.add(method + " cookie=" + (cookie ? "present" : "missing") + " thread="
                + (Thread.currentThread().isVirtual() ? "virtual" : "platform"));
        RuntimeException failure = this.failures.get(method);
        if (failure == null) {
            failure = this.failures.get("*");
        }
        if (failure != null) {
            throw failure;
        }
        return context;
    }

    static BackendUnavailableException unavailable(String service) {
        return new BackendUnavailableException(service, new ConnectException("Connection refused"));
    }

    private static void requireAdmin(AuthenticatedUser caller) {
        if (!caller.isAdmin()) {
            throw new BackendException(403, "Access denied", Map.of());
        }
    }

    private static <T> PageResponse<T> slice(List<T> all, int page, int size) {
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        int pages = (all.isEmpty()) ? 0 : (all.size() + size - 1) / size;
        return new PageResponse<>(all.subList(from, to), page, size, all.size(), pages, page == 0,
                page >= pages - 1);
    }

    private List<ChitEnrollmentResponse> enrollmentsOfUser(long userId) {
        return this.enrollments.stream().filter((e) -> e.userId() == userId && e.active()).toList();
    }

    private List<LoanResponse> loansOfUser(long userId) {
        return this.loans.stream().filter((loan) -> loan.userId() == userId).toList();
    }

    private String name(long userId) {
        return this.members.stream()
            .filter((member) -> member.userId() == userId)
            .map(MemberResponse::fullName)
            .findFirst()
            .orElse(null);
    }

    private ChitPlanResponse withCounts(ChitPlanResponse plan) {
        long filled = this.enrollments.stream().filter((e) -> e.planId().equals(plan.id()) && e.active()).count();
        long settled = this.cycles.getOrDefault(plan.id(), List.of()).stream().filter(ChitCycleResponse::closed)
            .count();
        return new ChitPlanResponse(plan.id(), plan.name(), plan.totalValue(), plan.durationMonths(),
                plan.commissionPercentage(), plan.commissionAmount(), plan.monthlyInstallment(), plan.maxBidLimit(),
                plan.totalMembers(), filled, settled, plan.paymentWindowDays(), plan.startDate(), plan.endDate(),
                plan.active(), plan.updatedAt());
    }

    // ------------------------------------------------------------------------------------------ sample data

    private void seed() {
        for (int i = 0; i < MEMBER_COUNT; i++) {
            String name = (i < NAMES.length) ? NAMES[i] : "Member " + String.format("%03d", i + 1);
            this.members.add(new MemberResponse((long) i + 1, name, "98" + String.format("%08d", 40012345 + i * 7919),
                    (i % 3 == 0) ? "S/o " + NAMES[(i + 5) % NAMES.length].split(" ")[0] : null,
                    (i == 0) ? "ADMIN" : "CUSTOMER"));
        }
        // the signed-in member's phone number must match their account
        this.members.set(3, new MemberResponse(4L, "Priya Raman", MEMBER.identifier(), "W/o Raman", "CUSTOMER"));

        ChitPlanResponse gold = plan(1, "Gold 1 lakh, January 2026", new BigDecimal("100000"), 20,
                new BigDecimal("5"), new BigDecimal("5000"), new BigDecimal("40000"), 20, LocalDate.of(2026, 1, 1));
        ChitPlanResponse silver = plan(2, "Silver 50,000, March 2026", new BigDecimal("50000"), 25,
                new BigDecimal("4"), new BigDecimal("2000"), new BigDecimal("20000"), 25, LocalDate.of(2026, 3, 5));
        ChitPlanResponse diwali = plan(3, "Diwali 2 lakh", new BigDecimal("200000"), 20, new BigDecimal("5"),
                new BigDecimal("10000"), new BigDecimal("80000"), 20, LocalDate.of(2026, 11, 1));
        for (ChitPlanResponse plan : List.of(gold, silver, diwali)) {
            this.plans.put(plan.id(), plan);
        }
        // Gold: 18 of 20 slots taken, Priya (4) holds slots 3 and 11, 6 months settled
        long user = 2;
        for (int slot = 1; slot <= 20; slot++) {
            if (slot == 9 || slot == 16) {
                continue;
            }
            long holder;
            if (slot == 3 || slot == 11) {
                holder = 4;
            }
            else {
                if (user == 4) {
                    user++;
                }
                holder = user++;
            }
            this.enrollments.add(new ChitEnrollmentResponse(100L + slot, gold.id(), gold.name(), holder,
                    name(holder), slot, true, Instant.parse("2025-12-20T05:30:00Z").plusSeconds(slot * 86400L)));
        }
        this.cycles.put(gold.id(), ledger(gold, 6, new long[] { 2, 5, 4, 7, 9, 12 },
                new String[] { "38000", "33500", "29000", "26000", "21500", "18000" }));
        this.cycles.put(silver.id(), ledger(silver, 0, new long[0], new String[0]));
        this.cycles.put(diwali.id(), ledger(diwali, 0, new long[0], new String[0]));

        this.loans.add(loan(12, 2, new BigDecimal("20000"), new BigDecimal("2000"), 100, LeaveType.SUNDAY,
                LocalDate.of(2026, 10, 10), true));
        this.loans.add(loan(11, 4, new BigDecimal("10000"), new BigDecimal("1000"), 100, LeaveType.NONE,
                LocalDate.of(2026, 9, 1), true));
        this.loans.add(loan(9, 13, new BigDecimal("15000"), new BigDecimal("1500"), 60, LeaveType.SUNDAY,
                LocalDate.of(2026, 2, 2), false));
        for (int i = 0; i < 12; i++) {
            boolean chit = i % 3 != 2;
            long userId = (i % 4 == 0) ? 4 : 2 + (i * 5) % 16;
            this.payments.add(new TransactionResponse("652f" + i, userId, name(userId),
                    chit ? TransactionType.CHIT_PAYMENT : TransactionType.LOAN_PAYMENT,
                    chit ? new BigDecimal(i % 2 == 0 ? "4650" : "5000") : new BigDecimal("200"),
                    LocalDate.of(2026, 10, 9).minusDays(i / 2), (i % 4 == 2) ? PaymentMode.UPI : PaymentMode.CASH,
                    "Office counter", chit ? 1L : 12L, (i == 1) ? "October installment" : null, 1L,
                    Instant.parse("2026-10-09T09:00:00Z").minusSeconds(i * 3600L)));
        }
    }

    private ChitPlanResponse plan(long id, String name, BigDecimal total, Integer months, BigDecimal pct,
            BigDecimal installment, BigDecimal maxBid, Integer members, LocalDate start) {
        BigDecimal commission = total.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new ChitPlanResponse(id, name, total, months, pct, commission, installment, maxBid, members, 0, 0,
                10, start, start.plusMonths(months), true, this.clock.instant());
    }

    private static List<ChitCycleResponse> ledger(ChitPlanResponse plan, int settledMonths, long[] winners,
            String[] bids) {
        List<ChitCycleResponse> ledger = new ArrayList<>();
        for (int month = 1; month <= plan.durationMonths(); month++) {
            LocalDate opens = plan.startDate().plusMonths(month - 1);
            boolean settled = month <= settledMonths;
            BigDecimal bid = settled ? new BigDecimal(bids[month - 1]) : BigDecimal.ZERO;
            BigDecimal dividend = settled ? bid.subtract(plan.commissionAmount())
                .divide(BigDecimal.valueOf(plan.totalMembers()), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            ledger.add(new ChitCycleResponse(1000L * plan.id() + month, plan.id(), month, opens, opens.plusDays(10),
                    bid, dividend, plan.monthlyInstallment().subtract(dividend), settled ? winners[month - 1] : null,
                    settled));
        }
        return Collections.synchronizedList(ledger);
    }

    private LoanResponse loan(long id, long userId, BigDecimal principal, BigDecimal interest, Integer days,
            LeaveType leave, LocalDate start, boolean active) {
        BigDecimal deduction = (interest != null) ? interest : BigDecimal.ZERO;
        int tenure = (days != null) ? days : 100;
        LocalDate first = (start != null) ? start : LocalDate.now(this.clock);
        return new LoanResponse(id, userId, name(userId), principal, deduction, principal.subtract(deduction),
                principal.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.HALF_UP), tenure,
                (leave != null) ? leave : LeaveType.NONE, first.minusDays(1), first,
                first.plusDays(tenure + tenure / 6), active, first.atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
