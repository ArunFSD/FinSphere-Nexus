package com.finsphere.chit.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation at {@code /swagger-ui.html} on the service's own port (7002). The session cookie scheme, the
 * error envelope and the standard error answers are added by common-library ({@code ApiDocsCustomizer}).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI chitFinanceOpenApi() {
        return new OpenAPI()
            .info(new Info().title("FinSphere Nexus: Chit and Finance API")
                .version("2.0")
                .contact(new Contact().name("FinSphere Nexus"))
                .description("""
                        Chit schemes, slot enrolments, monthly auctions, members, daily-collection loans and the \
                        payment log book.

                        **Sign in first**: open the Auth API page (http://localhost:7001/swagger-ui.html), run \
                        **POST /auth/login**, then come back here. The session cookie is shared by every port of the \
                        host, so the requests on this page are signed in. Without it every operation answers 401.

                        **Roles**: creating schemes, settling auctions, disbursing loans, recording payments and \
                        listing members need an ADMIN account. Members (CUSTOMER) see their own slots, loans and \
                        payments, and can book a free slot for themselves.

                        **Order of a typical run**: create a scheme (POST /chits/plans/create), book slots \
                        (POST /chits/enrollments/join), settle a month (POST /chits/cycles/auction), disburse a loan \
                        (POST /finance/loans/disburse), record payments (POST /finance/transactions/log).

                        **Answers** always use one envelope: `success`, `status`, `message`, `data` on success, \
                        `errors` (field name to message) on failure, and `correlationId` to quote in a support \
                        request. Lists are pages: `page` counts from 0 and `size` is at most 100."""))
            .servers(List.of(new Server().url("/").description("This service (direct, port 7002)")));
    }
}
