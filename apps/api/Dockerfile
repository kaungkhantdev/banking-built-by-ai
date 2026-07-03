# syntax=docker/dockerfile:1

###############################################################################
# Stage 1 — build the Spring Boot fat jar with Maven (Java 21)
###############################################################################
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependency layer: copy only the POM first so `mvn dependency:go-offline`
# is cached and re-run only when dependencies actually change.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# Source layer
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q clean package -DskipTests

###############################################################################
# Stage 2 — minimal JRE runtime
###############################################################################
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user
RUN groupadd --system bank && useradd --system --gid bank --home /app bank

COPY --from=build /build/target/backend-api-*.jar /app/app.jar
RUN chown -R bank:bank /app
USER bank

EXPOSE 8080

# Container-aware JVM defaults; override at runtime with JAVA_OPTS / SPRING_* envs.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
