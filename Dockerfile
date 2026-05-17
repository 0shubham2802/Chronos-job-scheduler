# ── Stage 1: Build ──────────────────────────────────────────────
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files first (better layer caching)
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# Download dependencies (cached unless build files change)
RUN gradle dependencies --no-daemon || true

# Copy source and build
COPY src ./src
RUN gradle bootJar -x test --no-daemon

# ── Stage 2: Run ────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S chronos && adduser -S chronos -G chronos

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership
RUN chown chronos:chronos app.jar

USER chronos

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
