package com.finsphere.frontend.flow;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.finsphere.common.security.AuthCookieService;
import com.finsphere.common.security.CookieProperties;
import com.finsphere.common.security.JwtAuthenticationFilter;
import com.finsphere.common.security.JwtProperties;
import com.finsphere.common.security.JwtService;
import com.finsphere.frontend.client.BackendCallContextResolver;
import com.finsphere.frontend.config.FrontendProperties;
import com.finsphere.frontend.config.FrontendSecurityConfig;
import com.finsphere.frontend.config.WebConfig;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.http.CacheControl;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.resource.VersionResourceResolver;

/**
 * The front end's own controllers, advice, security rules and templates, with a {@link StubBackend} in place of the
 * HTTP service clients. What Spring Boot would add (Thymeleaf view resolution, static resources with content-hash
 * URLs, ISO date binding) is declared here as config-repo/frontend-service.yml asks for it, so the test needs no
 * config server, registry or back ends.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebMvc
@EnableWebSecurity
@ComponentScan(basePackages = "com.finsphere.frontend.web")
@Import({ FrontendSecurityConfig.class, WebConfig.class, BackendCallContextResolver.class })
class FlowTestConfig implements WebMvcConfigurer {

    static final Instant NOW = Instant.parse("2026-10-09T13:30:00Z");

    @Bean
    Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Bean
    FrontendProperties frontendProperties() {
        return new FrontendProperties("Asia/Kolkata", "the Mylapore branch office, 044 2499 1234", 20);
    }

    @Bean
    JwtProperties jwtProperties() {
        return new JwtProperties("flow-test-secret-that-is-long-enough-1234", Duration.ofHours(24), "finsphere-nexus");
    }

    @Bean
    CookieProperties cookieProperties() {
        return new CookieProperties("fsn_auth_token", false, "Lax", "/", null);
    }

    @Bean
    JwtService jwtService(JwtProperties properties, Clock clock) {
        return new JwtService(properties, clock);
    }

    @Bean
    AuthCookieService authCookieService(CookieProperties cookieProperties, JwtProperties jwtProperties) {
        return new AuthCookieService(cookieProperties, jwtProperties.expiration());
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, AuthCookieService cookieService) {
        return new JwtAuthenticationFilter(jwtService, cookieService);
    }

    @Bean
    StubBackend backend(Clock clock, JwtService jwtService, AuthCookieService cookieService,
            BackendCallContextResolver resolver) {
        StubBackend backend = new StubBackend(clock, jwtService, cookieService);
        backend.setResolver(resolver);
        return backend;
    }

    // ------------------------------------------------------------------ what Spring Boot auto-configures

    @Bean
    SpringResourceTemplateResolver defaultTemplateResolver(ApplicationContext context) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(context);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        resolver.setCheckExistence(true);
        return resolver;
    }

    @Bean
    SpringTemplateEngine templateEngine(SpringResourceTemplateResolver resolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setEnableSpringELCompiler(false);
        engine.setRenderHiddenMarkersBeforeCheckboxes(false);
        return engine;
    }

    @Bean
    ThymeleafViewResolver thymeleafViewResolver(SpringTemplateEngine engine) {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(engine);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setContentType("text/html;charset=UTF-8");
        resolver.setProducePartialOutputWhileProcessing(true);
        resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
        return resolver;
    }

    @Bean
    ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
        return new ResourceUrlEncodingFilter();
    }

    /** spring.web.resources.chain.strategy.content and cache max-age 365d. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
            .resourceChain(true)
            .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
    }

    /** spring.mvc.format.date: iso */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(registry);
    }
}
