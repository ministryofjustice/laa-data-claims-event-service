## Data claims event service

[![Build main](https://github.com/ministryofjustice/laa-data-claims-event-service/actions/workflows/build-main.yml/badge.svg)](https://github.com/ministryofjustice/laa-data-claims-event-service/actions/workflows/build-main.yml)

## Project Structure

Includes the following subprojects:

- `data-claims-event-service` - Data claims event service which processes claims from an SQS queue.
- `reference-data-claim-api` - OpenAPI specification for the Data Claims API used for generating
  model classes.
- `reference-provider-details-api` - OpenAPI specification for the Provider Details API used for
  generating model classes.

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
- `docker-scripts/submit-bulk-submission.sh`

### Run application

`./gradlew bootRun`

`./gradlew bootRun --args='--spring.profiles.active=wiremock'`
