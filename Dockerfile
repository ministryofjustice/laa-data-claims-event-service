# Specify java runtime base image
FROM amazoncorretto:21-alpine

# Set up working directory in the container
RUN mkdir -p /opt/laa-spring-boot-microservice/
WORKDIR /opt/laa-spring-boot-microservice/

# Copy the JAR file into the container
COPY spring-boot-microservice-service/build/libs/spring-boot-microservice-service-1.0.0.jar app.jar

# Create a group and non-root user
RUN addgroup -S appgroup && adduser -u 1001 -S appuser -G appgroup

# Set the default user
USER 1001

# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
CMD java -jar app.jar