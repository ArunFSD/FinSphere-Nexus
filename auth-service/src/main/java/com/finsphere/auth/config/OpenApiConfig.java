package com.finsphere.auth.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation at {@code /swagger-ui.html} on the service's own port (7001). The session cookie scheme, the
 * error envelope and the standard error answers are added by common-library ({@code ApiDocsCustomizer}).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI authServiceOpenApi() {
        return new OpenAPI()
            .info(new Info().title("FinSphere Nexus: Auth API")
                .version("2.0")
                .contact(new Contact().name("FinSphere Nexus"))
                .description("""
                        Accounts and sessions: sign up, sign in, sign out, session checks and the signed-in profile.

                        **Try it in this page**
                        1. Open **POST /auth/login**, choose *Try it out* and send your mobile number (or e-mail) and \
                        password. The answer sets the `fsn_auth_token` session cookie (HttpOnly, so it does not \
                        appear in the page).
                        2. Every later request from this browser carries the cookie, including requests from the \
                        chit and finance API page on port 7002 (cookies are shared by every port of a host).
                        3. **POST /auth/logout** ends the session and removes the cookie.

                        **Answers** always use one envelope: `success`, `status`, `message`, `data` on success, \
                        `errors` (field name to message) on failure, and `correlationId` to quote in a support request.

                        **Limits**: five wrong passwords in a row pause sign-in for that account for 15 minutes \
                        (429). A database or Redis outage gives 503 within seconds instead of a hanging request."""))
            .servers(List.of(new Server().url("/").description("This service (direct, port 7001)")));
    }
}
