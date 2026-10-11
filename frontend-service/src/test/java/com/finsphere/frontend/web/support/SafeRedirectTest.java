package com.finsphere.frontend.web.support;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SafeRedirectTest {

    @Test
    void keepsLocalPaths() {
        assertEquals("/schemes/12", SafeRedirect.localPathOrHome("/schemes/12"));
        assertEquals("/payments?page=2", SafeRedirect.localPathOrHome("/payments?page=2"));
        assertEquals("/loginhelp", SafeRedirect.localPathOrHome("/loginhelp"), "only the sign-in pages themselves");
    }

    @Test
    void sendsEverythingElseHome() {
        for (String target : new String[] { null, "", "schemes", "//evil.example", "/\\evil.example",
                "https://evil.example", "/login", "/login?next=/x", "/logout", "/register", "/error", "/error/404",
                "/x\"onmouseover=alert(1)", "/" + "a".repeat(600) }) {
            assertEquals(SafeRedirect.HOME, SafeRedirect.localPathOrHome(target), String.valueOf(target));
        }
    }

    @Test
    void theFormPageComesFromTheRefererPathOnly() {
        assertEquals("/payments/new?member=4", SafeRedirect.formPage(post("http://localhost:7080/payments/new?member=4")));
        assertEquals("/schemes/1", SafeRedirect.formPage(post("https://evil.example/schemes/1")),
                "another site's address never leads off this site");
        assertNull(SafeRedirect.formPage(post(null)));
        assertNull(SafeRedirect.formPage(post("not a url at all")));
        assertNull(SafeRedirect.formPage(post("http://localhost//evil.example/x")));
    }

    @Test
    void returnPathIsThePageForGetsAndTheFormPageOtherwise() {
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/schemes/2");
        get.setQueryString("tab=ledger");
        assertEquals("/schemes/2?tab=ledger", SafeRedirect.returnPath(get));
        assertEquals("/payments/new?member=4", SafeRedirect.returnPath(post("http://localhost/payments/new?member=4")));
        assertEquals(SafeRedirect.HOME, SafeRedirect.returnPath(post("http://localhost/login?next=%2Floans")));
    }

    @Test
    void noticesReplaceAnyEarlierOne() {
        assertEquals("/schemes/1?notice=form-expired", SafeRedirect.withNotice("/schemes/1", Notice.FORM_EXPIRED));
        assertEquals("/login?next=%2Fschemes%2F1&notice=form-expired",
                SafeRedirect.withNotice("/login?next=%2Fschemes%2F1&notice=signed-out", Notice.FORM_EXPIRED));
    }

    private static MockHttpServletRequest post(String referer) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/payments");
        if (referer != null) {
            request.addHeader("Referer", referer);
        }
        return request;
    }
}
