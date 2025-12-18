# Build stage
FROM gradle:8-jdk21 AS builder

# 1. Declare that we expect these arguments
ARG GIT_PACKAGE_USER
ARG GIT_PACKAGE_KEY

# 2. Map them to the ENV variables Gradle is looking for
ENV GITHUB_ACTOR=${GIT_PACKAGE_USER}
ENV GITHUB_TOKEN=${GIT_PACKAGE_KEY}

# Set up working directory for build
WORKDIR /build

# Copy gradle files and source code
COPY . .

RUN env

# Run gradle build
RUN gradle data-claims-event-service:spotlessApply build -x test

# Debug step: List all JAR files to find the correct path
RUN find /build -name "*.jar"

# Runtime stage
FROM eclipse-temurin:21

# Set up working directory in the container
RUN mkdir -p /opt/data-claims-event-service/
WORKDIR /opt/data-claims-event-service/

# Copy the JAR file from builder stage
COPY --from=builder /build/data-claims-event-service/build/libs/data-claims-event-service-*-SNAPSHOT.jar app.jar

# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
CMD java -jar app.jar