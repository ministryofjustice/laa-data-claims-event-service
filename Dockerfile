# Runtime stage
FROM eclipse-temurin:25 AS base

# Set up working directory in the container
RUN mkdir -p /opt/data-claims-event-service/
WORKDIR /opt/data-claims-event-service/

COPY data-claims-event-service/build/libs/data-claims-event-service-1.0.0.jar app.jar

# Create a group and non-root user
RUN addgroup -S appgroup && adduser -u 1001 -S appuser -G appgroup

# Set the default user
USER 1001

# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]
