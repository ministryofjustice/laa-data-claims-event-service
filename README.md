## Data claims event service

[![Build main](https://github.com/ministryofjustice/laa-data-claims-event-service/actions/workflows/build-main.yml/badge.svg)](https://github.com/ministryofjustice/laa-data-claims-event-service/actions/workflows/build-main.yml)

## Prerequisites

- **Java 17 or later** - Required by Spring Boot 4.0.1
- **Docker** - For running LocalStack and Wiremock containers
- **Gradle 8.x** - Included via Gradle wrapper (`./gradlew`)
- **[laa-spring-boot-common](https://github.com/ministryofjustice/laa-spring-boot-common)** - Shared infrastructure and standards used across LAA projects


## Project Structure

Includes the following subprojects:

- `data-claims-event-service` - Data claims event service which processes claims from an SQS queue.
- `reference-provider-details-api` - OpenAPI specification for the Provider Details API used for
  generating model classes.

## Technology Stack

### Key Components

- **[Spring Boot 4.0.1](https://spring.io/projects/spring-boot)** - Application framework
- **[Spring Framework 7.0.2](https://spring.io/projects/spring-framework)** - Core framework
- **[Spring Cloud AWS 4.0.0](https://awspring.io/spring-cloud-aws/)** - AWS integration (SQS, LocalStack)
- **[Java 17+](https://www.oracle.com/java/technologies/downloads/)** - Minimum JDK version
- **[Gradle 8.x](https://gradle.org/)** - Build tool
- **[TestContainers 1.20.1](https://testcontainers.com/)** - Integration testing with Docker containers
- **[Sentry 8.31.0](https://docs.sentry.io/platforms/java/guides/spring-boot/)** - Error tracking and performance monitoring

## Development

For detailed information code quality and formatting when contributing to this project, see [DEVELOPMENT.md](DEVELOPMENT.md).

## Usage

### First time setup

The project uses the `laa-spring-boot-gradle-plugin`. Please follow the steps in
the [laa-spring-boot-common](https://github.com/ministryofjustice/laa-spring-boot-common?tab=readme-ov-file#provide-your-repository-credentials)
repository to set up your Github Packages credentials locally before building the application.

### Build application

`./gradlew clean build`

Includes checkstyle, spotless checks and unit tests.

### Run integration tests

`./gradlew integrationTest`

### Localstack & Wiremock

This project has dependencies on SQS, and various RESTful APIs. To run this project locally, an
instance of Localstack and three instances of Wiremock must be running. This can be achieved via
docker:

```shell
docker-compose up -d
```

To run the project using whilst depending on Wiremock, the `Wiremock` spring profile should be 
enabled.

#### SQS helper scripts
To aid with pushing test messages to the SQS queue, the following scripts are provided:
- `docker-scripts/view-messages-in-queue.sh`
- `docker-scripts/clear-queue.sh`
- `docker-scripts/publish-bulk-submission-event.sh`
- `docker-scripts/publish-submission-validation-event.sh`

### Set environment variables
The following environment variables are required to connect to Localstack SQS:
```sh
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_REGION=us-east-1
export BULK_CLAIM_QUEUE_NAME=claims-api-queue
```

### Run application

`./gradlew bootRun`

`./gradlew bootRun --args='--spring.profiles.active=wiremock'`
