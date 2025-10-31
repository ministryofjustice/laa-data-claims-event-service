# Specify java runtime base image
FROM eclipse-temurin:21.0.8_9-jre-ubi9-minimal

# Set up working directory in the container
RUN mkdir -p /opt/data-claims-event-service/
WORKDIR /opt/data-claims-event-service/

# Copy the JAR file into the container
COPY data-claims-event-service/build/libs/data-claims-event-service-1.0.0.jar app.jar

# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]