package com.finsphere.frontend.flow;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.web.CorrelationIdFilter;
import com.finsphere.frontend.config.WebConfig;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;

/**
 * One running front end (application context, servlet filters in the order Spring Boot registers them, MockMvc) and
 * browsers to drive it.
 */
final class FlowRig implements AutoCloseable {

    static final Pattern CSRF_FIELD = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    final AnnotationConfigWebApplicationContext context;

    final MockMvc mvc;

    final StubBackend backend;

    private FlowRig(AnnotationConfigWebApplicationContext context, MockMvc mvc) {
        this.context = context;
        this.mvc = mvc;
        this.backend = context.getBean(StubBackend.class);
    }

    static FlowRig start() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        MockServletContext servletContext = new MockServletContext();
        context.setServletContext(servletContext);
        context.register(FlowTestConfig.class);
        context.refresh();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(new CharacterEncodingFilter("UTF-8", true), new CorrelationIdFilter(),
                    WebConfig.trailingSlashFilter(), context.getBean("springSecurityFilterChain", Filter.class),
                    context.getBean(ResourceUrlEncodingFilter.class))
            .build();
        return new FlowRig(context, mvc);
    }

    Browser browser() {
        return new Browser();
    }

    Browser signedIn(AuthenticatedUser user, String password) throws Exception {
        Browser browser = new Browser();
        Page page = browser.submit("/login", "/login", form("identifier", user.identifier(), "password", password));
        if (page.status() != 302 || !browser.jar.containsKey("fsn_auth_token")) {
            throw new IllegalStateException("could not sign in " + user.identifier() + ": " + page.describe());
        }
        return browser;
    }

    @Override
    public void close() {
        this.context.close();
    }

    static Map<String, String> form(String... pairs) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            fields.put(pairs[i], pairs[i + 1]);
        }
        return fields;
    }

    /** A response, with the body decoded as UTF-8. */
    record Page(int status, String location, String body, MockHttpServletResponse raw) {

        String header(String name) {
            return this.raw.getHeader(name);
        }

        String describe() {
            String text = this.body.replaceAll("\\s+", " ");
            int alert = text.indexOf("class=\"alert");
            String excerpt = (alert >= 0) ? text.substring(alert, Math.min(text.length(), alert + 300))
                    : text.substring(0, Math.min(text.length(), 300));
            return "status " + this.status + ((this.location != null) ? " -> " + this.location : "") + " | " + excerpt;
        }
    }

    /** Keeps cookies like a browser and submits forms with the CSRF token found in the page that holds them. */
    final class Browser {

        final Map<String, String> jar = new LinkedHashMap<>();

        Page get(String path) throws Exception {
            URI uri = URI.create(path);
            return send(MockMvcRequestBuilders.get(uri), uri, true);
        }

        /** For concurrent use: leaves the cookie jar alone. */
        Page getUnsaved(String path) throws Exception {
            URI uri = URI.create(path);
            return send(MockMvcRequestBuilders.get(uri), uri, false);
        }

        /** Opens the page, then submits the form whose action is {@code action}, as a person would. */
        Page submit(String page, String action, Map<String, String> fields) throws Exception {
            return postFrom(page, action, fields, csrfFrom(page, action));
        }

        /** Sends a form that sits on {@code page} with the given token, with the Referer header browsers add. */
        Page postFrom(String page, String action, Map<String, String> fields, String token) throws Exception {
            Map<String, String> withToken = new LinkedHashMap<>(fields);
            withToken.put("_csrf", token);
            return post(action, withToken, "http://localhost" + page);
        }

        String csrfFrom(String page, String action) throws Exception {
            Page response = get(page);
            int form = response.body().indexOf("action=\"" + action + "\"");
            if (form < 0) {
                throw new IllegalStateException("no form for " + action + " on " + page + ": " + response.describe());
            }
            Matcher token = CSRF_FIELD.matcher(response.body());
            if (!token.find(form)) {
                throw new IllegalStateException("no CSRF field in the form for " + action + " on " + page);
            }
            return token.group(1);
        }

        Page postWithToken(String action, Map<String, String> fields, String token) throws Exception {
            Map<String, String> withToken = new LinkedHashMap<>(fields);
            withToken.put("_csrf", token);
            return post(action, withToken);
        }

        Page post(String action, Map<String, String> fields) throws Exception {
            return post(action, fields, null);
        }

        Page post(String action, Map<String, String> fields, @Nullable String referer) throws Exception {
            URI uri = URI.create(action);
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);
            fields.forEach(request::param);
            if (referer != null) {
                request.header("Referer", referer);
            }
            return send(request, uri, true);
        }

        private Page send(MockHttpServletRequestBuilder request, URI uri, boolean keepCookies) throws Exception {
            // As Tomcat reports it for the DispatcherServlet mapped to "/": the servlet path is the whole path
            if (!uri.getPath().endsWith("/")) {
                request.servletPath(uri.getPath());
            }
            request.header("User-Agent", "Mozilla/5.0 (flow test)");
            List<Cookie> cookies = new ArrayList<>();
            synchronized (this.jar) {
                this.jar.forEach((name, value) -> cookies.add(new Cookie(name, value)));
            }
            if (!cookies.isEmpty()) {
                request.cookie(cookies.toArray(Cookie[]::new));
            }
            MockHttpServletResponse response = FlowRig.this.mvc.perform(request).andReturn().getResponse();
            if (keepCookies) {
                synchronized (this.jar) {
                    for (Cookie cookie : response.getCookies()) {
                        if (cookie.getMaxAge() == 0 || cookie.getValue() == null || cookie.getValue().isEmpty()) {
                            this.jar.remove(cookie.getName());
                        }
                        else {
                            this.jar.put(cookie.getName(), cookie.getValue());
                        }
                    }
                }
            }
            return new Page(response.getStatus(), response.getHeader("Location"),
                    response.getContentAsString(StandardCharsets.UTF_8), response);
        }
    }
}
