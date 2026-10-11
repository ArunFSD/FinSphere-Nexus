package com.finsphere.common.docs;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiDocsCustomizerTest {

    private final ApiDocsCustomizer customizer = new ApiDocsCustomizer("fsn_auth_token");

    @Test
    void theSessionCookieIsTheSecuritySchemeAndTheErrorEnvelopeASchema() {
        OpenAPI api = document();
        this.customizer.customise(api);

        SecurityScheme cookie = api.getComponents().getSecuritySchemes().get(ApiDocsCustomizer.SESSION_COOKIE);
        assertEquals(SecurityScheme.Type.APIKEY, cookie.getType());
        assertEquals(SecurityScheme.In.COOKIE, cookie.getIn());
        assertEquals("fsn_auth_token", cookie.getName());
        assertEquals(List.of(ApiDocsCustomizer.SESSION_COOKIE), List.copyOf(api.getSecurity().get(0).keySet()));

        Schema<?> error = api.getComponents().getSchemas().get(ApiDocsCustomizer.ERROR_SCHEMA);
        assertTrue(error.getProperties().keySet().containsAll(List.of("success", "status", "message", "errors",
                "path", "correlationId", "timestamp")));
    }

    @Test
    void everyOperationListsTheErrorsThatCanHappenThere() {
        OpenAPI api = document();
        this.customizer.customise(api);

        ApiResponses create = operation(api, "/chits/plans/create", PathItem.HttpMethod.POST).getResponses();
        assertEquals(List.of("201", "400", "401", "403", "409", "500", "503"), List.copyOf(create.keySet()),
                "a change: validation, session, role, conflicts and outages");

        ApiResponses detail = operation(api, "/chits/plans/{planId}", PathItem.HttpMethod.GET).getResponses();
        assertEquals(List.of("200", "400", "401", "403", "404", "500", "503"), List.copyOf(detail.keySet()),
                "a read by id: a bad id, a missing record");

        ApiResponses login = operation(api, "/auth/login", PathItem.HttpMethod.POST).getResponses();
        assertEquals("Wrong password", login.get("401").getDescription(), "an operation's own answer is kept");
        assertFalse(login.containsKey("403"), "public operations get no role errors");

        Object example = detail.get("404").getContent().get("application/json").getExample();
        assertTrue(example instanceof Map<?, ?> body && Boolean.FALSE.equals(body.get("success"))
                && "/chits/plans/{planId}".equals(body.get("path")), String.valueOf(example));
        assertEquals("#/components/schemas/ApiError",
                detail.get("503").getContent().get("application/json").getSchema().get$ref());
    }

    @Test
    void customisingTwiceChangesNothing() {
        OpenAPI api = document();
        this.customizer.customise(api);
        int answers = operation(api, "/chits/plans/create", PathItem.HttpMethod.POST).getResponses().size();
        this.customizer.customise(api);
        assertEquals(answers, operation(api, "/chits/plans/create", PathItem.HttpMethod.POST).getResponses().size());
        assertEquals(1, api.getSecurity().size());
    }

    private static OpenAPI document() {
        Operation create = new Operation().responses(new ApiResponses()
            .addApiResponse("201", new ApiResponse().description("Created")));
        Operation detail = new Operation()
            .addParametersItem(new Parameter().in("path").name("planId").required(true))
            .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("The scheme")));
        Operation login = new Operation().security(List.of())
            .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("Signed in"))
                .addApiResponse("401", new ApiResponse().description("Wrong password")));
        Paths paths = new Paths();
        paths.addPathItem("/chits/plans/create", new PathItem().post(create));
        paths.addPathItem("/chits/plans/{planId}", new PathItem().get(detail));
        paths.addPathItem("/auth/login", new PathItem().post(login));
        return new OpenAPI().paths(paths);
    }

    private static Operation operation(OpenAPI api, String path, PathItem.HttpMethod method) {
        Operation operation = api.getPaths().get(path).readOperationsMap().get(method);
        assertNotNull(operation, path);
        return operation;
    }
}
