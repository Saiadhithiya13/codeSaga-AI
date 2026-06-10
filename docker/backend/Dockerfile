# ─────────────────────────────────────────────────────────────────────────────
# CodeSage AI — Multi-Stage Dockerfile
# Stage 1: Build with JDK 24 + Maven
# Stage 2: Run with JRE 24 Alpine (minimal attack surface)
# ─────────────────────────────────────────────────────────────────────────────

# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-24 AS builder

WORKDIR /build

# Copy dependency manifests first for Docker layer caching
COPY pom.xml .
COPY .mvn/ .mvn/
COPY src ./src

RUN mvn clean package -DskipTests

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jre AS runtime

# Create non-root user (security best practice)
RUN groupadd -r codesage && useradd -r -g codesage codesage

WORKDIR /app

# Create log directory with correct permissions
RUN mkdir -p /app/logs && chown -R codesage:codesage /app

# Copy the fat JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Switch to non-root user
USER codesage

# Expose application port
EXPOSE 8080

# ─── Health Check ─────────────────────────────────────────────────────────────
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/api/v1/health || exit 1

# ─── JVM Tuning for Containers ───────────────────────────────────────────────
# - UseContainerSupport: respect cgroup CPU/memory limits
# - MaxRAMPercentage: use 75% of container memory for heap
# - Virtual threads (configured via spring.threads.virtual.enabled=true in YAML)
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseZGC \
    -XX:+ZGenerational \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
