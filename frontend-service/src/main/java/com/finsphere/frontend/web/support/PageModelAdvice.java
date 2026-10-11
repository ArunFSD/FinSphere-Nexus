package com.finsphere.frontend.web.support;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.common.security.AuthCookieService;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.common.web.CorrelationId;
import com.finsphere.frontend.client.BackendException;
import com.finsphere.frontend.client.BackendUnavailableException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Model attributes every page uses ({@code me}, {@code fmt}, {@code notice}) and the translation of failures into
 * pages: an ended session goes back to sign-in (and returns to the page afterwards), missing records and missing
 * permissions get their error pages, an unreachable service gets a page that says which one, and a link with a value
 * that cannot be read is either corrected or treated as a page that does not exist.
 */
@ControllerAdvice(basePackages = "com.finsphere.frontend.web")
public class PageModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(PageModelAdvice.class);

    /** After a save whose outcome is unknown: retrying blindly could record a payment or a loan twice. */
    private static final String CHECK_BEFORE_RETRY = "Your change may still have been saved, so check the list "
            + "before you try again.";

    private final Formats formats;

    private final AuthCookieService cookieService;

    public PageModelAdvice(Formats formats, AuthCookieService cookieService) {
        this.formats = formats;
        this.cookieService = cookieService;
    }

    @ModelAttribute("me")
    public @Nullable AuthenticatedUser me() {
        return SecurityUtils.currentUser().orElse(null);
    }

    @ModelAttribute("fmt")
    public Formats fmt() {
        return this.formats;
    }

    @ModelAttribute("notice")
    public @Nullable Notice notice(@RequestParam(name = "notice", required = false) @Nullable String key) {
        return Notice.of(key);
    }

    @ExceptionHandler(BackendException.class)
    public ModelAndView backendError(BackendException ex, HttpServletRequest request, HttpServletResponse response) {
        if (ex.is(HttpStatus.UNAUTHORIZED)) {
            this.cookieService.clear(response);
            return new ModelAndView(signInAgain(request));
        }
        if (ex.is(HttpStatus.FORBIDDEN)) {
            return errorPage("error/403", HttpStatus.FORBIDDEN, ex.userMessage());
        }
        if (ex.is(HttpStatus.NOT_FOUND)) {
            return errorPage("error/404", HttpStatus.NOT_FOUND, ex.userMessage());
        }
        log.warn("Back-end call failed for {} {}: {} {}", request.getMethod(), request.getRequestURI(),
                ex.getStatus(), ex.getMessage());
        if (ex.isServiceTrouble()) {
            String service = (ex.getService() != null) ? "The " + Services.describe(ex.getService()) : "A service";
            return errorPage("error/unavailable", HttpStatus.SERVICE_UNAVAILABLE, service
                    + " could not finish this request: a database it needs is not answering, or it took too long. "
                    + (isSave(request) ? CHECK_BEFORE_RETRY : "Try again in a minute."));
        }
        return errorPage("error/general", HttpStatus.BAD_GATEWAY, ex.userMessage());
    }

    @ExceptionHandler(BackendUnavailableException.class)
    public ModelAndView backendUnavailable(BackendUnavailableException ex, HttpServletRequest request) {
        log.warn("{} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        String service = Services.describe(ex.getService());
        String message;
        if (!isSave(request)) {
            message = "We could not reach the " + service + ". It may be starting up or restarting. "
                    + "Try again in a minute.";
        }
        else if (ex.outcomeUnknown()) {
            message = "The " + service + " did not answer in time. " + CHECK_BEFORE_RETRY;
        }
        else {
            message = "We could not reach the " + service + ", so nothing was saved. It may be starting up or "
                    + "restarting. Try again in a minute.";
        }
        return errorPage("error/unavailable", HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    /**
     * The answer broke off while it was being read (the read timeout or a dropped connection), or it was not what
     * the client expected. Which service sent it is not known here.
     */
    @ExceptionHandler(RestClientException.class)
    public ModelAndView unreadableAnswer(RestClientException ex, HttpServletRequest request) {
        log.warn("{} {}: answer could not be read: {}", request.getMethod(), request.getRequestURI(), ex.toString());
        if (ex.contains(IOException.class)) {
            return errorPage("error/unavailable", HttpStatus.SERVICE_UNAVAILABLE, "A service stopped answering. "
                    + (isSave(request) ? CHECK_BEFORE_RETRY : "Try again in a minute."));
        }
        return errorPage("error/general", HttpStatus.BAD_GATEWAY,
                "A service sent an answer we could not read. Try again in a minute.");
    }

    /**
     * A value in the link that cannot be read. In the path ({@code /schemes/abc}) it names a record that cannot exist,
     * so the visitor gets the "not found" page. In the query of a page ({@code /members?page=x},
     * {@code /loans/new?member=x}) the value is dropped and the page opens with its default instead.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView unreadableValue(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        if (ex.getParameter().hasParameterAnnotation(RequestParam.class)
                && HttpMethod.GET.matches(request.getMethod())) {
            String withoutValue = UriComponentsBuilder.fromPath(SafeRedirect.pathWithinApplication(request))
                .query(request.getQueryString())
                .replaceQueryParam(ex.getName())
                .build()
                .toUriString();
            return new ModelAndView("redirect:" + withoutValue);
        }
        return errorPage("error/404", HttpStatus.NOT_FOUND, null);
    }

    private static boolean isSave(HttpServletRequest request) {
        return !HttpMethod.GET.matches(request.getMethod());
    }

    /** Sign in again, then come back here (or to the page that held the form, after a failed save). */
    private static String signInAgain(HttpServletRequest request) {
        String next = SafeRedirect.returnPath(request);
        return "redirect:/login?notice=" + Notice.SESSION_ENDED.key()
                + (SafeRedirect.HOME.equals(next) ? "" : "&next=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
    }

    private static ModelAndView errorPage(String view, HttpStatus status, @Nullable String message) {
        ModelAndView page = new ModelAndView(view, status);
        page.addObject("status", status.value());
        page.addObject("message", message);
        page.addObject("detail", message);
        page.addObject("correlationId", CorrelationId.current());
        return page;
    }
}
