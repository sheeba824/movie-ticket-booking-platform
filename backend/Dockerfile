# Dockerfile for Java Spring Boot Microservices
# Build: docker build -t movie-tickets/booking-service:latest -f backend/Dockerfile backend/
# Run: docker run -p 8804:8804 movie-tickets/booking-service:latest

# Multi-stage build for optimal image size
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY backend/src ./backend/src

# Build application
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    ls -la target/

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy built application from builder stage
COPY --from=builder /app/backend/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    chown -R appuser:appuser /app

USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8804/actuator/health || exit 1

# Expose port
EXPOSE 8804 8004

# JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dspring.profiles.active=production"

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
