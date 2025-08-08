## Data claims event service

[![Build main](https://github.com/ministryofjustice/laa-submit-a-bulk-claim-api/actions/workflows/build-main.yml/badge.svg)](https://github.com/ministryofjustice/laa-submit-a-bulk-claim-api/actions/workflows/build-main.yml)

## Project Structure
Includes the following subprojects:

- `bulk-claim-api` - OpenAPI specification used for generating API interfaces and documentation.
- `bulk-claim-service` - REST API service with business logic for handling of bulk claim submission requests.
- `provider-details-api-reference` - OpenAPI specification for the Provider Details API used for generating model classes used by the Provider Details API client.

## Usage

### First time setup

The project uses the `laa-ccms-spring-boot-gradle-plugin`. Please follow the steps in the [laa-ccms-spring-boot-common](https://github.com/ministryofjustice/laa-ccms-spring-boot-common?tab=readme-ov-file#provide-your-repository-credentials) repository to set up your Github Packages credentials locally before building the application.

### Build application
`./gradlew clean build`

Includes checkstyle, spotless checks and unit tests.

### Run integration tests
`./gradlew integrationTest`

### Run application
`./gradlew bootRun`

### Run application via Docker
`docker compose up`

## Application Endpoints

### API Documentation

#### Swagger UI
- http://localhost:8080/swagger-ui/index.html

#### API docs (JSON)
- http://localhost:8080/v3/api-docs

### Actuator Endpoints
The following actuator endpoints have been configured:
- http://localhost:8080/actuator
- http://localhost:8080/actuator/health
