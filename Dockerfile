# Build stage
# Default to external building
ARG BUILD_SOURCE=external

FROM gradle:9-jdk25 AS builder

# Set up working directory for build
WORKDIR /build

# Copy gradle files and source code
COPY . .

RUN env

# Run gradle build
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    export GITHUB_ACTOR="$(cat /run/secrets/github_actor)" && \
    export GITHUB_TOKEN="$(cat /run/secrets/github_token)" && \
    gradle assemble

# Debug step: List all JAR files to find the correct path
RUN find /build -name "*.jar"

# Runtime stage
FROM eclipse-temurin:25 AS base

# Set up working directory in the container
RUN mkdir -p /opt/data-claims-event-service/
WORKDIR /opt/data-claims-event-service/

# --- Stage for copying from the internal builder stage ---
FROM base AS build-internal
# Copy the JAR file from builder stage
COPY --from=builder /build/data-claims-event-service/build/libs/data-claims-event-service-*-SNAPSHOT.jar app.jar

# --- Stage for copying from the local filesystem (CI/Manual) ---
FROM base AS build-external
COPY data-claims-event-service/build/libs/data-claims-event-service-1.0.0.jar app.jar

# Create a group and non-root user
RUN addgroup -S appgroup && adduser -u 1001 -S appuser -G appgroup

# Set the default user
USER 1001

# --- Final Stage ---
ARG BUILD_SOURCE
FROM build-${BUILD_SOURCE}


# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]
