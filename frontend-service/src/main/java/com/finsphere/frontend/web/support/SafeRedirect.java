package com.finsphere.frontend.web.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Where people are sent back to. Every "return to" path is checked to be a path on this site, so a crafted link
 * cannot redirect anyone to another site.
 */
public final class SafeRedirect {

    public static final String HOME = "/dashboard";

    private static final Pattern LOCAL_PATH = Pattern.compile("^/(?![/\\\\])[A-Za-z0-9/_\\-.~?=&%#]*$");

    /** Pages nobody wants to land on after signing in. */
    private static final Pattern NOT_A_DESTINATION = Pattern.compile("^/(?:login|logout|register|error)(?:[/?#].*)?$");

    private SafeRedirect() {
    }

    /** {@code candidate} when it is a page of this site worth returning to after signing in, else the overview. */
    public static String localPathOrHome(@Nullable String candidate) {
        String path = localPath(candidate);
        return (path == null || NOT_A_DESTINATION.matcher(path).matches()) ? HOME : path;
    }

    /** {@code candidate} when it is a path (with an optional query) on this site, else null. */
    public static @Nullable String localPath(@Nullable String candidate) {
        if (candidate == null || candidate.length() > 512 || !LOCAL_PATH.matcher(candidate).matches()) {
            return null;
        }
        return candidate;
    }

    /**
     * The page a form was sent from: path and query of the {@code Referer} header, which browsers send with
     * same-site requests (see the Referrer-Policy header). Only the path is used, so the result is always a page of
     * this site. Null when the header is missing or unusable.
     */
    public static @Nullable String formPage(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.length() > 1024) {
            return null;
        }
        try {
            URI uri = new URI(referer);
            String path = uri.getRawPath();
            String contextPath = request.getContextPath();
            if (path == null || !path.startsWith(contextPath + "/")) {
                return null;
            }
            String query = uri.getRawQuery();
            return localPath(path.substring(contextPath.length()) + ((query != null) ? "?" + query : ""));
        }
        catch (URISyntaxException ex) {
            return null;
        }
    }

    /**
     * Where to come back to after signing in again: the page itself for a GET, the page that held the form for
     * anything else (so a session that ends while a form is open brings the person back to that form).
     */
    public static String returnPath(HttpServletRequest request) {
        if (HttpMethod.GET.matches(request.getMethod())) {
            String query = request.getQueryString();
            return localPathOrHome(pathWithinApplication(request) + ((query != null) ? "?" + query : ""));
        }
        return localPathOrHome(formPage(request));
    }

    /** {@code path} with its {@code notice} query parameter set to {@code notice} (any earlier one is replaced). */
    public static String withNotice(String path, Notice notice) {
        return UriComponentsBuilder.fromUriString(path)
            .replaceQueryParam("notice", notice.key())
            .build()
            .toUriString();
    }

    /** The request path without the context path, still URL-encoded as the browser sent it. */
    public static String pathWithinApplication(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
