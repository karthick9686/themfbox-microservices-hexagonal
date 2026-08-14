# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Dependencies resolve in their own layer, so a source-only change does not
# re-download the world.
COPY pom.xml lombok.config ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY config ./config
COPY src ./src

# Tests and quality gates run in CI, not here — repeating them would roughly triple
# image build time without adding a signal. `mvn verify` is the gate; this is packaging.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests \
 && mv target/*.jar app.jar

# ---------------------------------------------------------------------------
# Run
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

# Never root. The service reads a database and serves HTTP; it has no reason to own
# anything in the filesystem.
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
COPY --from=build --chown=app:app /build/app.jar app.jar

USER app

# Matches the prod default in application-prod.properties. Override with SERVER_PORT.
EXPOSE 8090
ENV SERVER_PORT=8090 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Credentials are supplied at runtime and have no defaults — the container exits
# immediately with a message naming what is missing. See ADR-0006.
#   docker run --env-file .env -p 8090:8090 investor-portfolio-hexagonal

# springdoc is on the classpath, so /v3/api-docs answers without adding actuator
# purely to be pinged. busybox provides wget in this base image.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q --spider "http://localhost:${SERVER_PORT}/v3/api-docs" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
