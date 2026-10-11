# syntax=docker/dockerfile:1.7
# One image recipe for every service. The build stage compiles the whole project once and is shared by all
# services (Docker caches it); the runtime stage picks one module:
#   docker build -f docker/Dockerfile --build-arg MODULE=auth-service -t finsphere/auth-service .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package \
 && for module in config-server discovery-server gateway-service auth-service chit-finance-service frontend-service; do \
      java -Djarmode=tools -jar "$module/target/$module-2.0.0-SNAPSHOT.jar" extract --layers \
        --application-filename app.jar --destination "/extracted/$module" || exit 1; \
    done

FROM eclipse-temurin:25-jre AS runtime
ARG MODULE
RUN groupadd --system --gid 10001 finsphere \
 && useradd --system --uid 10001 --gid finsphere --home-dir /app --shell /usr/sbin/nologin finsphere \
 && mkdir -p /app/logs && chown finsphere:finsphere /app/logs
WORKDIR /app
# Layers from least to most frequently changing, so a code change only rebuilds the last ones
COPY --from=build /extracted/${MODULE}/dependencies/ ./
COPY --from=build /extracted/${MODULE}/snapshot-dependencies/ ./
COPY --from=build /extracted/${MODULE}/application/ ./
COPY --chmod=0755 docker/healthcheck.sh /usr/local/bin/healthcheck
USER finsphere
ENV FINSPHERE_LOGGING_DIR=/app/logs
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
