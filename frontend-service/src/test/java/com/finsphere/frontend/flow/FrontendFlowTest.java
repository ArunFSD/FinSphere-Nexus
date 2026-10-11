package com.finsphere.frontend.flow;

import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.finsphere.common.dto.auth.RegistrationRequest;
import com.finsphere.common.dto.chit.AuctionRequest;
import com.finsphere.common.dto.chit.ChitPlanRequest;
import com.finsphere.common.dto.chit.EnrollmentRequest;
import com.finsphere.common.dto.finance.LoanRequest;
import com.finsphere.common.dto.finance.TransactionRequest;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.JwtProperties;
import com.finsphere.common.security.JwtService;
import com.finsphere.frontend.client.BackendException;
import com.finsphere.frontend.client.BackendUnavailableException;
import com.finsphere.frontend.flow.FlowRig.Browser;
import com.finsphere.frontend.flow.FlowRig.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.finsphere.frontend.flow.FlowRig.form;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The web app driven like a browser: the real Spring Security filter chain (CSRF, session cookie, role rules), the
 * real controllers, form binding, error advice and Thymeleaf templates, against a stub of the back-end services.
 */
class FrontendFlowTest {

    private static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "font-src 'self'; connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'self'; "
            + "object-src 'none'";

    private FlowRig rig;

    private StubBackend backend;

    @BeforeEach
    void start() {
        this.rig = FlowRig.start();
        this.backend = this.rig.backend;
    }

    @AfterEach
    void stop() {
        this.rig.close();
    }

    // ------------------------------------------------------------------------------------------ signed out

    @Test
    void signedOutVisitorsAreSentToSignInAndComeBackAfterwards() throws Exception {
        Browser browser = this.rig.browser();
        expect(browser.get("/"), 302, "/login");
        expect(browser.get("/dashboard"), 302, "/login");
        expect(browser.get("/schemes/1?tab=ledger"), 302, "/login?next=%2Fschemes%2F1%3Ftab%3Dledger");
        assertEquals(302, browser.get("/no-such-page").status());

        Page login = browser.get("/login");
        assertEquals(200, login.status(), login.describe());
        assertTrue(login.body().contains("action=\"/login\""), login.describe());
        assertTrue(FlowRig.CSRF_FIELD.matcher(login.body()).find(), "sign-in form carries a CSRF token");
        assertTrue(browser.jar.containsKey("XSRF-TOKEN"), "CSRF cookie is set");
        assertNoTemplateSyntax(login);

        Page help = browser.get("/forgot-password");
        assertTrue(help.body().contains("the Mylapore branch office, 044 2499 1234"), help.describe());
    }

    @Test
    void pagesCarrySecurityHeadersAndContentHashedAssets() throws Exception {
        Browser browser = this.rig.browser();
        Page login = browser.get("/login");
        assertEquals(CSP, login.header("Content-Security-Policy"));
        assertEquals("strict-origin-when-cross-origin", login.header("Referrer-Policy"));
        assertEquals("DENY", login.header("X-Frame-Options"));

        Matcher stylesheet = Pattern.compile("href=\"(/css/app-[0-9a-f]{32}\\.css)\"").matcher(login.body());
        assertTrue(stylesheet.find(), "stylesheet is linked by content hash");
        Page css = browser.get(stylesheet.group(1));
        assertEquals(200, css.status());
        assertTrue(String.valueOf(css.header("Cache-Control")).contains("max-age=31536000"), "cached for a year");
        Matcher script = Pattern.compile("src=\"(/js/app-[0-9a-f]{32}\\.js)\"").matcher(login.body());
        assertTrue(script.find(), "script is linked by content hash");
        assertEquals(200, browser.get(script.group(1)).status());
        Matcher font = Pattern.compile("(/fonts/[a-z-]+-var(?:-[0-9a-f]{32})?\\.woff2)").matcher(css.body());
        assertTrue(font.find(), "stylesheet references the self-hosted fonts");
        assertEquals(200, browser.get(font.group(1)).status());
        assertEquals(200, browser.get("/favicon.svg").status());
    }

    @Test
    void signUpCreatesMemberAccountsOnly() throws Exception {
        Browser browser = this.rig.browser();
        Page created = browser.submit("/register", "/register", form("fullName", "Kumar Velu", "phoneNumber",
                "+91 98400 77777", "email", "", "password", "Velu12345", "confirmPassword", "Velu12345", "address",
                "Adyar, Chennai", "careOf", "S/o Velu", "role", "ADMIN"));
        expect(created, 302, "/login?notice=registered");
        RegistrationRequest request = lastRequest(RegistrationRequest.class);
        assertEquals("CUSTOMER", request.role(), "sign-up never creates an administrator");
        assertEquals("9840077777", request.phoneNumber());
        assertEquals(null, request.email(), "a blank e-mail is sent as none");

        Page mismatch = browser.submit("/register", "/register", form("fullName", "Kumar Velu", "phoneNumber",
                "9840077778", "password", "Velu12345", "confirmPassword", "Velu12346", "address", "Adyar"));
        assertEquals(400, mismatch.status());
        assertTrue(mismatch.body().contains("The two passwords do not match"), mismatch.describe());
        assertFalse(mismatch.body().contains("Velu12345"), "passwords are never echoed back");

        Page duplicate = browser.submit("/register", "/register", form("fullName", "Kumar Velu", "phoneNumber",
                "9840077777", "password", "Velu12345", "confirmPassword", "Velu12345", "address", "Adyar"));
        assertEquals(409, duplicate.status());
        assertTrue(duplicate.body().contains("This mobile number already has an account"), duplicate.describe());
    }

    // ------------------------------------------------------------------------------------------ signing in

    @Test
    void signInChecksTheCsrfTokenAndTheCredentials() throws Exception {
        Browser browser = this.rig.browser();
        browser.get("/login");
        expect(browser.post("/login", form("identifier", StubBackend.ADMIN.identifier(), "password",
                StubBackend.ADMIN_PASSWORD)), 302, "/login?notice=form-expired");
        assertFalse(browser.jar.containsKey("fsn_auth_token"), "sign-in without the CSRF token is refused");
        assertTrue(browser.get("/login?notice=form-expired").body().contains("nothing was saved"));

        Page wrong = browser.submit("/login", "/login", form("identifier", StubBackend.ADMIN.identifier(),
                "password", "nope"));
        assertEquals(401, wrong.status());
        assertTrue(wrong.body().contains("The mobile number, e-mail or password is not correct."), wrong.describe());

        Page blank = browser.submit("/login", "/login", form("identifier", " ", "password", ""));
        assertEquals(400, blank.status());
        assertTrue(blank.body().contains("Enter your mobile number or e-mail and your password."), blank.describe());

        this.backend.failures.put("login", StubBackend.unavailable("auth-service"));
        Page down = this.rig.browser().submit("/login", "/login", form("identifier",
                StubBackend.ADMIN.identifier(), "password", StubBackend.ADMIN_PASSWORD));
        assertEquals(503, down.status());
        assertTrue(down.body().contains("Signing in is not available right now."), down.describe());

        // auth-service runs, but Redis or its database does not answer
        this.backend.failures.put("login", new BackendException(503, "A backing service is temporarily unavailable.",
                Map.of(), "auth-service"));
        Page busy = this.rig.browser().submit("/login", "/login", form("identifier",
                StubBackend.ADMIN.identifier(), "password", StubBackend.ADMIN_PASSWORD));
        assertEquals(503, busy.status());
        assertTrue(busy.body().contains("Signing in is not available right now."), busy.describe());
    }

    @Test
    void theCsrfTokenIsRenewedWhenThePersonChangesAndOtherwiseKept() throws Exception {
        Browser browser = this.rig.browser();
        browser.get("/login");
        String beforeSignIn = browser.jar.get("XSRF-TOKEN");
        assertTrue(beforeSignIn != null, "the sign-in page issues a token");
        expect(browser.submit("/login", "/login", form("identifier", StubBackend.ADMIN.identifier(), "password",
                StubBackend.ADMIN_PASSWORD)), 302, "/dashboard");
        assertFalse(browser.jar.containsKey("XSRF-TOKEN"), "signing in drops the token");

        // Several tabs: each form still works after other pages were opened and used
        String slotTab = browser.csrfFrom("/schemes/1", "/schemes/1/slots");
        String signedInToken = browser.jar.get("XSRF-TOKEN");
        assertFalse(signedInToken.equals(beforeSignIn), "a new token after signing in");
        String paymentTab = browser.csrfFrom("/payments/new?member=4", "/payments");
        browser.get("/dashboard");
        browser.get("/loans");
        expect(browser.postFrom("/payments/new?member=4", "/payments", payment("CHIT_PAYMENT:1"), paymentTab), 302,
                "/payments?notice=payment-recorded");
        expect(browser.postFrom("/schemes/1", "/schemes/1/slots", form("slotNumber", "9", "userId", "6"), slotTab),
                302, "/schemes/1?notice=slot-booked#slots");
        assertEquals(signedInToken, browser.jar.get("XSRF-TOKEN"), "kept for the whole session");

        expect(browser.submit("/dashboard", "/logout", form()), 302, "/login?notice=signed-out");
        assertFalse(browser.jar.containsKey("XSRF-TOKEN"), "signing out drops the token");
    }

    @Test
    void signInOnlyRedirectsWithinTheSite() throws Exception {
        expect(signIn("https://evil.example/steal"), 302, "/dashboard");
        expect(signIn("//evil.example"), 302, "/dashboard");
        expect(signIn("/schemes/2"), 302, "/schemes/2");

        Browser browser = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        expect(browser.get("/login?next=/loans"), 302, "/loans");
    }

    @Test
    void signOutEndsTheSession() throws Exception {
        Browser browser = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        Page out = browser.submit("/dashboard", "/logout", form());
        expect(out, 302, "/login?notice=signed-out");
        assertFalse(browser.jar.containsKey("fsn_auth_token"), "session cookie is cleared");
        assertTrue(browser.get("/login?notice=signed-out").body().contains("You have signed out."));
        assertEquals(302, browser.get("/dashboard").status());
    }

    // ------------------------------------------------------------------------------------------ administrator

    @Test
    void administratorDashboardLoadsItsSectionsInParallel() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        this.backend.calls.clear();
        Page dashboard = admin.get("/dashboard");
        assertEquals(200, dashboard.status(), dashboard.describe());
        assertTrue(dashboard.body().contains("Running schemes"));
        assertTrue(dashboard.body().contains("Gold 1 lakh, January 2026"));
        assertNoTemplateSyntax(dashboard);
        assertEquals(4, this.backend.calls.size(), this.backend.calls::toString);
        assertTrue(this.backend.calls.stream().allMatch((call) -> call.endsWith("cookie=present thread=virtual")),
                () -> "every section call runs on a virtual thread with the session: " + this.backend.calls);

        assertTrue(admin.get("/dashboard?notice=slot-booked").body().contains("Slot booked."));
        Page forged = admin.get("/dashboard?notice=%3Cscript%3Ealert(1)%3C%2Fscript%3E");
        assertFalse(forged.body().contains("alert(1)"), "unknown notices are ignored");
    }

    @Test
    void administratorRunsAScheme() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        Page schemes = admin.get("/schemes");
        assertTrue(schemes.body().contains("Silver 50,000, March 2026"), schemes.describe());
        Page scheme = admin.get("/schemes/1");
        assertTrue(scheme.body().contains("action=\"/schemes/1/slots\""), scheme.describe());
        assertTrue(scheme.body().contains("action=\"/schemes/1/auction\""), scheme.describe());

        expect(admin.submit("/schemes/1", "/schemes/1/slots", form("slotNumber", "9", "userId", "6")), 302,
                "/schemes/1?notice=slot-booked#slots");
        EnrollmentRequest booking = lastRequest(EnrollmentRequest.class);
        assertEquals(6L, booking.userId());
        assertEquals(9, booking.slotNumber());

        Page taken = admin.submit("/schemes/1", "/schemes/1/slots", form("slotNumber", "9", "userId", "7"));
        assertEquals(409, taken.status());
        assertTrue(taken.body().contains("That slot is already taken."), taken.describe());
        Page notANumber = admin.submit("/schemes/1", "/schemes/1/slots", form("slotNumber", "nine", "userId", "7"));
        assertEquals(400, notANumber.status());
        assertTrue(notANumber.body().contains("Choose a free slot"), notANumber.describe());

        // A member the scheme service does not know yet is a form error next to the member, not a missing page
        this.backend.failures.put("enroll", new BackendException(404, "Member not found", Map.of("userId",
                "No active member with id 99. New registrations appear here within a few seconds.")));
        Page unknown = admin.submit("/schemes/1", "/schemes/1/slots", form("slotNumber", "10", "userId", "99"));
        assertEquals(404, unknown.status());
        assertTrue(unknown.body().contains("New registrations appear here within a few seconds."), unknown.describe());
        assertTrue(unknown.body().contains("action=\"/schemes/1/slots\""), "the booking form is shown again");
        this.backend.failures.clear();

        expect(admin.submit("/schemes/1", "/schemes/1/auction", form("monthlyCount", "7", "winnerUserId", "6",
                "bidAmount", "15000")), 302, "/schemes/1?notice=auction-settled#ledger");
        AuctionRequest auction = lastRequest(AuctionRequest.class);
        assertEquals(7, auction.monthlyCount());
        assertEquals(15000, auction.bidAmount().intValue());
        Page tooHigh = admin.submit("/schemes/1", "/schemes/1/auction", form("monthlyCount", "8", "winnerUserId",
                "6", "bidAmount", "50000"));
        assertEquals(400, tooHigh.status());
        assertTrue(tooHigh.body().contains("The bid is above the scheme"), tooHigh.describe());
        Page incomplete = admin.submit("/schemes/1", "/schemes/1/auction", form("monthlyCount", "8", "bidAmount",
                ""));
        assertEquals(400, incomplete.status());
        assertTrue(incomplete.body().contains("Choose the month and the winner"), incomplete.describe());
    }

    @Test
    void administratorCreatesASchemeAndSeesFieldErrors() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        assertEquals(200, admin.get("/schemes/new").status());
        Page invalid = admin.submit("/schemes/new", "/schemes", scheme("", "100000", "2026-11-01"));
        assertEquals(400, invalid.status());
        assertTrue(invalid.body().contains("Enter a scheme name"), invalid.describe());
        Page garbage = admin.submit("/schemes/new", "/schemes", scheme("Pongal 1 lakh", "1 lakh", "2026-11-01"));
        assertEquals(400, garbage.status());
        assertTrue(garbage.body().contains("&quot;1 lakh&quot; is not a valid value here"), garbage.describe());

        Page created = admin.submit("/schemes/new", "/schemes", scheme("Pongal 1 lakh", "100000", "2027-01-14"));
        assertEquals(302, created.status(), created.describe());
        assertTrue(created.location().matches("/schemes/\\d+\\?notice=scheme-created"), created.location());
        assertEquals(LocalDate.of(2027, 1, 14), lastRequest(ChitPlanRequest.class).startDate());
        Page fresh = admin.get(created.location());
        assertTrue(fresh.body().contains("Pongal 1 lakh"), fresh.describe());
        assertTrue(fresh.body().contains("Scheme created"), fresh.describe());
    }

    @Test
    void memberPickersListEveryMemberNotJustTheFirstPage() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        for (String page : List.of("/schemes/1", "/loans/new?member=4", "/payments/new")) {
            Page form = admin.get(page);
            assertEquals(200, form.status(), form.describe());
            assertTrue(form.body().contains("Member 101") && form.body().contains("Member 230"),
                    () -> page + " lists all " + StubBackend.MEMBER_COUNT + " members");
        }
    }

    @Test
    void administratorManagesMembers() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        assertTrue(admin.get("/members").body().contains("Lakshmi Narayanan"));
        assertTrue(admin.get("/members?page=11").body().contains("Member 230"), "last page of the directory");
        assertTrue(admin.get("/members/new").body().contains("value=\"ADMIN\""), "role choice is offered");

        expect(admin.submit("/members/new", "/members", form("fullName", "முருகன் செல்வம்", "phoneNumber",
                "9790012345", "password", "Murugan123", "confirmPassword", "Murugan123", "address", "T. Nagar",
                "careOf", "S/o Selvam", "role", "ADMIN")), 302, "/members?notice=member-added");
        RegistrationRequest added = lastRequest(RegistrationRequest.class);
        assertEquals("ADMIN", added.role());
        assertEquals("முருகன் செல்வம்", added.fullName());

        admin.submit("/members/new", "/members", form("fullName", "Siva", "phoneNumber", "9790012346", "password",
                "Sivaa1234", "confirmPassword", "Sivaa1234", "address", "Adyar", "role", "SUPERUSER"));
        assertEquals("CUSTOMER", lastRequest(RegistrationRequest.class).role(), "unknown roles fall back to member");
    }

    @Test
    void administratorIssuesLoansAndRecordsPayments() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        assertTrue(admin.get("/loans").body().contains("Lakshmi Narayanan"));
        expect(admin.submit("/loans/new?member=4", "/loans", form("userId", "4", "principalAmount", "10000",
                "interestDeduction", "1000", "tenureDays", "100", "startDate", "2026-10-12", "leaveType", "SUNDAY")),
                302, "/loans?notice=loan-disbursed");
        assertEquals(LocalDate.of(2026, 10, 12), lastRequest(LoanRequest.class).startDate());
        Page badDate = admin.submit("/loans/new", "/loans", form("userId", "4", "principalAmount", "10000",
                "interestDeduction", "0", "tenureDays", "100", "startDate", "12/10/2026", "leaveType", "SUNDAY"));
        assertEquals(400, badDate.status());
        assertTrue(badDate.body().contains("is not a valid value here"), badDate.describe());

        assertEquals(200, admin.get("/payments").status());
        Page targets = admin.get("/payments/new?member=4");
        assertTrue(targets.body().contains("Chit: Gold 1 lakh, January 2026"), targets.describe());
        assertTrue(targets.body().contains("Loan 11"), targets.describe());
        expect(admin.submit("/payments/new?member=4", "/payments", payment("CHIT_PAYMENT:1")), 302,
                "/payments?notice=payment-recorded");
        TransactionRequest recorded = lastRequest(TransactionRequest.class);
        assertEquals(1L, recorded.targetId());
        assertEquals(null, recorded.remarks(), "blank remarks are sent as none");

        Page wrongLoan = admin.submit("/payments/new?member=4", "/payments", payment("LOAN_PAYMENT:999"));
        assertEquals(400, wrongLoan.status());
        assertTrue(wrongLoan.body().contains("This payment does not match"), wrongLoan.describe());
        Page forged = admin.submit("/payments/new?member=4", "/payments", payment("DROP TABLE"));
        assertEquals(400, forged.status());
        assertTrue(forged.body().contains("Choose what this payment is for"), forged.describe());

        assertTrue(admin.get("/account").body().contains("ravi@finsphere.in"));
        assertEquals(404, admin.get("/no-such-page").status());
    }

    // ------------------------------------------------------------------------------------------ member

    @Test
    void membersSeeAndBookOnlyForThemselves() throws Exception {
        Browser member = this.rig.signedIn(StubBackend.MEMBER, StubBackend.MEMBER_PASSWORD);
        this.backend.calls.clear();
        Page dashboard = member.get("/dashboard");
        assertTrue(dashboard.body().contains("Your slots"), dashboard.describe());
        assertEquals(4, this.backend.calls.size(), this.backend.calls::toString);
        assertTrue(this.backend.calls.stream().allMatch((call) -> call.endsWith("cookie=present thread=virtual")),
                this.backend.calls::toString);

        Page scheme = member.get("/schemes/1");
        assertTrue(scheme.body().contains("Your slots here"), scheme.describe());
        assertFalse(scheme.body().contains("Lakshmi Narayanan"), "other members' names stay hidden");
        assertFalse(scheme.body().contains("action=\"/schemes/1/auction\""), "no auction form for members");

        expect(member.submit("/schemes/1", "/schemes/1/slots", form("slotNumber", "16", "userId", "6")), 302,
                "/schemes/1?notice=slot-booked#slots");
        assertEquals(4L, lastRequest(EnrollmentRequest.class).userId(), "members always book for themselves");

        assertTrue(member.get("/loans").body().contains("10,000"));
        assertEquals(200, member.get("/payments").status());
        assertTrue(member.get("/account").body().contains("W/o Raman"));
    }

    @Test
    void membersCannotUseAdministratorPages() throws Exception {
        Browser member = this.rig.signedIn(StubBackend.MEMBER, StubBackend.MEMBER_PASSWORD);
        for (String page : List.of("/members", "/members/new", "/schemes/new", "/loans/new", "/payments/new")) {
            assertEquals(403, member.get(page).status(), page);
        }
        String token = member.csrfFrom("/schemes/1", "/logout");
        Page auction = member.postWithToken("/schemes/1/auction", form("monthlyCount", "8", "winnerUserId", "4",
                "bidAmount", "1000"), token);
        assertEquals(403, auction.status(), auction.describe());
        Page scheme = member.postWithToken("/schemes", form("name", "Mine"), token);
        assertEquals(403, scheme.status(), scheme.describe());
    }

    // ------------------------------------------------------------------------------------------ failures

    @Test
    void backEndFailuresBecomeHelpfulPages() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);

        this.backend.failures.put("plan", StubBackend.unavailable("chit-finance-service"));
        Page down = admin.get("/schemes/1");
        assertEquals(503, down.status());
        assertTrue(down.body().contains("schemes and payments service"), down.describe());
        this.backend.failures.clear();

        this.backend.failures.put("payments", StubBackend.unavailable("chit-finance-service"));
        Page partial = admin.get("/dashboard");
        assertEquals(200, partial.status(), "the dashboard still renders");
        assertTrue(partial.body().contains("Gold 1 lakh, January 2026"), partial.describe());
        assertTrue(partial.body().contains("We could not reach the schemes and payments service"),
                partial.describe());
        this.backend.failures.clear();

        this.backend.failures.put("plan", new BackendException(500, "NullPointerException at line 42", Map.of()));
        Page broken = admin.get("/schemes/1");
        assertEquals(502, broken.status());
        assertFalse(broken.body().contains("NullPointerException"), "internal details stay hidden");
        this.backend.failures.clear();

        // the service runs, but its database does not answer
        this.backend.failures.put("plan", new BackendException(503, "A backing service is temporarily unavailable.",
                Map.of(), "chit-finance-service"));
        Page busy = admin.get("/schemes/1");
        assertEquals(503, busy.status());
        assertTrue(busy.body().contains("The schemes and payments service could not finish this request"),
                busy.describe());
        this.backend.failures.clear();

        // a save that never reached the service, and one whose answer did not come back in time
        this.backend.failures.put("recordPayment", StubBackend.unavailable("chit-finance-service"));
        Page unsaved = admin.submit("/payments/new?member=4", "/payments", payment("CHIT_PAYMENT:1"));
        assertEquals(503, unsaved.status());
        assertTrue(unsaved.body().contains("so nothing was saved"), unsaved.describe());
        this.backend.failures.put("recordPayment", new BackendUnavailableException("chit-finance-service",
                new HttpTimeoutException("request timed out")));
        Page unknown = admin.submit("/payments/new?member=4", "/payments", payment("CHIT_PAYMENT:1"));
        assertEquals(503, unknown.status());
        assertTrue(unknown.body().contains("may still have been saved, so check the list"), unknown.describe());
        this.backend.failures.clear();

        assertEquals(404, admin.get("/schemes/999").status());

        this.backend.failures.put("activePlans", new BackendException(401, null, Map.of()));
        expect(admin.get("/schemes"), 302, "/login?notice=session-ended&next=%2Fschemes");
        assertFalse(admin.jar.containsKey("fsn_auth_token"), "the ended session is cleared");
        this.backend.failures.clear();

        Browser member = this.rig.signedIn(StubBackend.MEMBER, StubBackend.MEMBER_PASSWORD);
        this.backend.failures.put("myLoans", new BackendException(401, null, Map.of()));
        Page section = member.get("/dashboard");
        assertEquals(302, section.status());
        assertTrue(section.location().startsWith("/login?notice=session-ended"), section.location());
    }

    @Test
    void forgedExpiredOrForeignSessionCookiesAreRejected() throws Exception {
        JwtProperties properties = this.rig.context.getBean(JwtProperties.class);
        JwtService twoDaysAgo = new JwtService(properties, Clock.fixed(FlowTestConfig.NOW.minus(Duration.ofDays(2)),
                ZoneOffset.UTC));
        Browser expired = this.rig.browser();
        expired.jar.put("fsn_auth_token", twoDaysAgo.issue(StubBackend.ADMIN));
        expect(expired.get("/dashboard"), 302, "/login?notice=session-ended");
        assertFalse(expired.jar.containsKey("fsn_auth_token"), "the expired cookie is removed");

        String token = new JwtService(properties, Clock.fixed(FlowTestConfig.NOW, ZoneOffset.UTC))
            .issue(new AuthenticatedUser(4L, StubBackend.MEMBER.identifier(), "CUSTOMER", "Priya Raman", null));
        String[] parts = token.split("\\.");
        String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
            .replace("\"CUSTOMER\"", "\"ADMIN\"");
        Browser forged = this.rig.browser();
        forged.jar.put("fsn_auth_token", parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + "." + parts[2]);
        expect(forged.get("/members"), 302, "/login?notice=session-ended&next=%2Fmembers");

        JwtService otherKey = new JwtService(new JwtProperties("a-different-secret-that-is-also-long-enough",
                Duration.ofHours(1), "finsphere-nexus"), Clock.fixed(FlowTestConfig.NOW, ZoneOffset.UTC));
        Browser foreign = this.rig.browser();
        foreign.jar.put("fsn_auth_token", otherKey.issue(StubBackend.ADMIN));
        expect(foreign.get("/dashboard"), 302, "/login?notice=session-ended");
    }

    // ------------------------------------------------------------------------------------------ redirects

    @Test
    void formOnlyAddressesOpenedDirectlyLeadSomewhereUseful() throws Exception {
        Browser signedOut = this.rig.browser();
        expect(signedOut.get("/logout"), 302, "/login");

        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        Page confirm = admin.get("/logout");
        assertEquals(200, confirm.status(), confirm.describe());
        assertTrue(confirm.body().contains("Sign out?") && confirm.body().contains("Ravi Kumar"), confirm.describe());
        assertTrue(admin.jar.containsKey("fsn_auth_token"), "opening the address alone does not sign out");
        expect(admin.submit("/logout", "/logout", form()), 302, "/login?notice=signed-out");
        assertFalse(admin.jar.containsKey("fsn_auth_token"));

        Browser member = this.rig.signedIn(StubBackend.MEMBER, StubBackend.MEMBER_PASSWORD);
        expect(member.get("/schemes/1/slots"), 302, "/schemes/1#slots");
        expect(member.get("/schemes/1/auction"), 302, "/schemes/1#auction");

        // A sign-out form left open after the session ended still signs out cleanly, without calling the service
        Browser ended = this.rig.browser();
        String token = ended.csrfFrom("/login", "/login");
        this.backend.calls.clear();
        expect(ended.postFrom("/dashboard", "/logout", form(), token), 302, "/login?notice=signed-out");
        assertTrue(this.backend.calls.isEmpty(), this.backend.calls::toString);
    }

    @Test
    void addressesAreNormalised() throws Exception {
        Browser signedOut = this.rig.browser();
        expect(signedOut.get("/dashboard/"), 308, "/dashboard");
        expect(signedOut.get("/"), 302, "/login");

        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        expect(admin.get("/schemes/"), 308, "/schemes");
        expect(admin.get("/schemes/1/?notice=slot-booked"), 308, "/schemes/1?notice=slot-booked");
        assertEquals(200, admin.get("/schemes").status());

        // a value that cannot be read: a record that cannot exist, or a query value that is dropped
        assertEquals(404, admin.get("/schemes/first").status());
        expect(admin.get("/members?page=two"), 302, "/members");
        expect(admin.get("/members?notice=member-added&page=two"), 302, "/members?notice=member-added");
        expect(admin.get("/payments/new?member=priya"), 302, "/payments/new");

        // page numbers past the end go to the last page, so a list never looks empty while it has entries
        expect(admin.get("/members?page=99"), 302, "/members?page=11");
        assertTrue(admin.get("/members?page=11").body().contains("Member 230"));
        expect(admin.get("/schemes?page=3"), 302, "/schemes");
        assertEquals(200, admin.get("/members?page=-1").status(), "a negative page is the first page");
    }

    @Test
    void aSessionThatEndsWhileAFormIsOpenComesBackToThatForm() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        String token = admin.csrfFrom("/payments/new?member=4", "/payments");

        // the cookie expires while the form is open
        JwtService yesterday = new JwtService(this.rig.context.getBean(JwtProperties.class),
                Clock.fixed(FlowTestConfig.NOW.minus(Duration.ofDays(2)), ZoneOffset.UTC));
        admin.jar.put("fsn_auth_token", yesterday.issue(StubBackend.ADMIN));
        int before = this.backend.requests.size();
        Page sent = admin.postFrom("/payments/new?member=4", "/payments", payment("CHIT_PAYMENT:1"), token);
        expect(sent, 302, "/login?notice=session-ended&next=%2Fpayments%2Fnew%3Fmember%3D4");
        assertEquals(before, this.backend.requests.size(), "nothing reached the back end");
        assertFalse(admin.jar.containsKey("fsn_auth_token"), "the expired cookie is removed");

        Page login = admin.get(sent.location());
        assertTrue(login.body().contains("Your session ended."), login.describe());
        assertTrue(login.body().contains("value=\"/payments/new?member=4\""), "the form page is kept");
        expect(admin.submit(sent.location(), "/login", form("identifier", StubBackend.ADMIN.identifier(), "password",
                StubBackend.ADMIN_PASSWORD, "next", "/payments/new?member=4")), 302, "/payments/new?member=4");

        // the same when the service is the one that says the session ended
        this.backend.failures.put("recordPayment", new BackendException(401, null, Map.of()));
        expect(admin.submit("/payments/new?member=4", "/payments", payment("CHIT_PAYMENT:1")), 302,
                "/login?notice=session-ended&next=%2Fpayments%2Fnew%3Fmember%3D4");
        this.backend.failures.clear();

        // signed out in another tab while this form was open (which also dropped the CSRF token)
        Browser tabs = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        String slotToken = tabs.csrfFrom("/schemes/1", "/schemes/1/slots");
        expect(tabs.submit("/dashboard", "/logout", form()), 302, "/login?notice=signed-out");
        expect(tabs.postFrom("/schemes/1", "/schemes/1/slots", form("slotNumber", "12", "userId", "6"), slotToken),
                302, "/login?notice=session-ended&next=%2Fschemes%2F1");
        expect(tabs.postFrom("/dashboard", "/logout", form(), slotToken), 302, "/login?notice=signed-out");
    }

    @Test
    void anExpiredFormGoesBackToItsPageAndSavesNothing() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        admin.get("/schemes/1");
        int before = this.backend.requests.size();
        Page sent = admin.postFrom("/schemes/1?notice=slot-booked", "/schemes/1/slots",
                form("slotNumber", "12", "userId", "6"), "a-token-from-before-the-cookies-were-cleared");
        expect(sent, 302, "/schemes/1?notice=form-expired");
        assertEquals(before, this.backend.requests.size(), "nothing reached the back end");
        Page back = admin.get(sent.location());
        assertTrue(back.body().contains("This page had expired, so nothing was saved."), back.describe());
        assertTrue(back.body().contains("alert alert-error"), "shown as a warning, not a success");

        Browser visitor = this.rig.browser();
        visitor.get("/login?next=%2Fschemes%2F1");
        expect(visitor.post("/login", form("identifier", "x", "password", "y"), "http://localhost/login?next=%2Fschemes%2F1"),
                302, "/login?next=%2Fschemes%2F1&notice=form-expired");
    }

    // ------------------------------------------------------------------------------------------ loading screen

    @Test
    void everyPageHasTheLoadingScreenAndFormsSayWhatTheyWaitFor() throws Exception {
        Page login = this.rig.browser().get("/login");
        assertLoadingScreen(login);
        assertTrue(login.body().contains("data-loading=\"Signing you in…\""), login.describe());

        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        Map<String, List<String>> waits = Map.of(
                "/dashboard", List.of("Signing you out…"),
                "/schemes/1", List.of("Booking the slot…", "Settling the auction…"),
                "/schemes/new", List.of("Creating the scheme and its monthly schedule…"),
                "/members/new", List.of("Adding the member…"),
                "/loans/new", List.of("Disbursing the loan…"),
                "/payments/new?member=4", List.of("Loading their schemes and loans…", "Recording the payment…"),
                "/logout", List.of("Signing you out…"));
        for (Map.Entry<String, List<String>> page : waits.entrySet()) {
            Page shown = admin.get(page.getKey());
            assertEquals(200, shown.status(), shown.describe());
            assertLoadingScreen(shown);
            for (String wait : page.getValue()) {
                assertTrue(shown.body().contains("data-loading=\"" + wait + "\""), () -> page.getKey() + ": " + wait);
            }
        }
    }

    @Test
    void concurrentPageLoadsAllSucceed() throws Exception {
        Browser admin = this.rig.signedIn(StubBackend.ADMIN, StubBackend.ADMIN_PASSWORD);
        Browser member = this.rig.signedIn(StubBackend.MEMBER, StubBackend.MEMBER_PASSWORD);
        List<Future<Integer>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < 120; i++) {
                Browser browser = (i % 2 == 0) ? admin : member;
                String path = switch (i % 4) {
                    case 0, 1 -> "/dashboard";
                    case 2 -> "/schemes/1";
                    default -> "/payments";
                };
                results.add(pool.submit(() -> browser.getUnsaved(path).status()));
            }
        }
        for (Future<Integer> result : results) {
            assertEquals(200, result.get());
        }
    }

    // ------------------------------------------------------------------------------------------ helpers

    private Page signIn(String next) throws Exception {
        return this.rig.browser().submit("/login", "/login", form("identifier", StubBackend.ADMIN.identifier(),
                "password", StubBackend.ADMIN_PASSWORD, "next", next));
    }

    private static Map<String, String> scheme(String name, String totalValue, String startDate) {
        return form("name", name, "totalValue", totalValue, "durationMonths", "20", "commissionPercentage", "5",
                "monthlyInstallment", "5000", "maxBidLimit", "40000", "totalMembers", "20", "startDate", startDate,
                "paymentDuration", "10");
    }

    private static Map<String, String> payment(String target) {
        return form("userId", "4", "target", target, "amount", "4650", "paymentDate", "2026-10-09", "paymentMode",
                "CASH", "receivedByInfo", "Office counter", "remarks", "");
    }

    private static void expect(Page page, int status, String location) {
        assertEquals(status, page.status(), page.describe());
        assertEquals(location, page.location(), page.describe());
    }

    /** The screen sits outside the page region it covers, once per page, with its stop button and announcer. */
    private static void assertLoadingScreen(Page page) {
        String body = page.body();
        assertEquals(1, occurrences(body, "data-loading-screen"), page::describe);
        assertEquals(1, occurrences(body, "data-loading-region"), page::describe);
        assertEquals(1, occurrences(body, "data-loading-stop"), page::describe);
        assertEquals(1, occurrences(body, "data-loading-announce"), page::describe);
        assertTrue(body.indexOf("data-loading-screen") > body.lastIndexOf("</main>"), "the screen is outside main");
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + token.length())) {
            count++;
        }
        return count;
    }

    private static void assertNoTemplateSyntax(Page page) {
        for (String marker : List.of("th:", "${", "*{", "#{", "~{")) {
            assertFalse(page.body().contains(marker), () -> "unprocessed template syntax " + marker);
        }
    }

    private <T> T lastRequest(Class<T> type) {
        for (int i = this.backend.requests.size() - 1; i >= 0; i--) {
            if (type.isInstance(this.backend.requests.get(i))) {
                return type.cast(this.backend.requests.get(i));
            }
        }
        return fail("no " + type.getSimpleName() + " reached the back end");
    }
}
