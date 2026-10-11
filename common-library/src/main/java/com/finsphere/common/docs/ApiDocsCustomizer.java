package com.finsphere.common.docs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;

/**
 * Conventions every FinSphere API document shares, so Swagger UI shows what can go wrong next to what goes right:
 * <ul>
 * <li>the session cookie as the security scheme (operations marked public with an empty
 * {@code @SecurityRequirements} are left open),</li>
 * <li>the error envelope as the {@value #ERROR_SCHEMA} schema,</li>
 * <li>the standard error answers on every operation that can produce them (an answer an operation documents itself
 * is kept).</li>
 * </ul>
 */
public final class ApiDocsCustomizer implements GlobalOpenApiCustomizer {

    /** Name of the security scheme for the session cookie. */
    public static final String SESSION_COOKIE = "sessionCookie";

    /** Name of the error envelope schema. */
    public static final String ERROR_SCHEMA = "ApiError";

    private static final String JSON = "application/json";

    private static final Set<PathItem.HttpMethod> WRITES = Set.of(PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT,
            PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE);

    private final String cookieName;

    public ApiDocsCustomizer(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public void customise(OpenAPI openApi) {
        Components components = (openApi.getComponents() != null) ? openApi.getComponents() : new Components();
        openApi.setComponents(components);
        components.addSecuritySchemes(SESSION_COOKIE, new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.COOKIE)
            .name(this.cookieName)
            .description("HttpOnly session cookie set by POST /auth/login and removed by POST /auth/logout. "
                    + "In Swagger UI, call /auth/login once in this browser: every later request to any FinSphere "
                    + "service on the same host sends the cookie automatically. Other clients keep the cookie from the "
                    + "login answer (for example curl -c cookies.txt, then -b cookies.txt)."));
        components.addSchemas(ERROR_SCHEMA, errorSchema());
        if (openApi.getSecurity() == null || openApi.getSecurity().isEmpty()) {
            openApi.addSecurityItem(new SecurityRequirement().addList(SESSION_COOKIE));
        }
        if (openApi.getPaths() != null) {
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap()
                .forEach((method, operation) -> addErrorAnswers(path, method, operation)));
        }
    }

    private static void addErrorAnswers(String path, PathItem.HttpMethod method, Operation operation) {
        ApiResponses answers = (operation.getResponses() != null) ? operation.getResponses() : new ApiResponses();
        operation.setResponses(answers);
        boolean open = operation.getSecurity() != null && operation.getSecurity().isEmpty();
        boolean write = WRITES.contains(method);
        boolean parameters = operation.getParameters() != null && !operation.getParameters().isEmpty();
        if (write || parameters) {
            add(answers, "400", "The request is not valid. `errors` names each field that needs changing.",
                    example(400, "Some details need changing. Check the highlighted fields.",
                            Map.of("amount", "must be greater than 0"), path));
        }
        if (!open) {
            add(answers, "401", "No session cookie, or the session has expired or was signed out. "
                    + "Sign in with POST /auth/login.", example(401, "Authentication required", null, path));
            add(answers, "403", "Signed in, but this account may not do this (most changes need the ADMIN role, "
                    + "members may only see and book for themselves).",
                    example(403, "You do not have permission to perform this action", null, path));
        }
        if (path.contains("{")) {
            add(answers, "404", "There is no record with that id.",
                    example(404, "Scheme not found", Map.of("planId", "No scheme with id 42"), path));
        }
        if (write) {
            add(answers, "409", "Conflicts with existing data, for example a slot that was just taken or a mobile "
                    + "number that already has an account.", example(409, "The request conflicts with existing data",
                            null, path));
        }
        add(answers, "500", "Unexpected failure. Quote `correlationId` when reporting it: it is in the server logs "
                + "and in the X-Correlation-Id header.", example(500, "An unexpected system error occurred", null,
                        path));
        add(answers, "503", "A database, cache or broker this service needs is not answering, or the request ran past "
                + "its time limit (15 s for database work) and was cancelled. Try again shortly.",
                example(503, "A backing service is temporarily unavailable. Please try again shortly", null, path));
    }

    private static void add(ApiResponses answers, String status, String description, Map<String, Object> example) {
        if (answers.containsKey(status)) {
            return;
        }
        answers.addApiResponse(status, new ApiResponse().description(description)
            .content(new Content().addMediaType(JSON, new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA))
                .example(example))));
    }

    private static Map<String, Object> example(int status, String message, @Nullable Map<String, String> errors,
            String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("status", status);
        body.put("message", message);
        if (errors != null) {
            body.put("errors", errors);
        }
        body.put("path", path);
        body.put("correlationId", "3f1c9a7e2b4d4c1a");
        body.put("timestamp", "2026-10-10T05:30:00Z");
        return body;
    }

    private static Schema<?> errorSchema() {
        Schema<?> schema = new ObjectSchema()
            .description("Every error answer has this shape: the same envelope as successful answers, with "
                    + "success=false and no data.")
            .addProperty("success", new BooleanSchema().example(false))
            .addProperty("status", new IntegerSchema().example(409).description("The HTTP status, repeated"))
            .addProperty("message", new StringSchema().example("That slot is already taken.")
                .description("What went wrong, written to be shown to people"))
            .addProperty("errors", new MapSchema().additionalProperties(new StringSchema())
                .description("Field name to message, for validation and business rule failures")
                .example(Map.of("slotNumber", "Slot 9 was just taken by another member")))
            .addProperty("path", new StringSchema().example("/chits/enrollments/join"))
            .addProperty("correlationId", new StringSchema().example("3f1c9a7e2b4d4c1a")
                .description("Also in the X-Correlation-Id header and in every log line of this request"))
            .addProperty("timestamp", new DateTimeSchema().description("When the answer was produced (UTC)"));
        schema.setRequired(List.of("success", "status", "message", "timestamp"));
        return schema;
    }
}
