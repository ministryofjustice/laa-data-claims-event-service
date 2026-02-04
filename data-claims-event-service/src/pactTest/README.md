# Pact Testing Documentation

## Overview

Pact tests in this project implement consumer-driven contract testing between services. These tests ensure
that service interactions remain compatible as they evolve independently.

## How It Works

PactTest utilizes MockWebServer to simulate the Claims API during testing. The test suite:

1. Creates mock interactions
2. Verifies consumer expectations
3. Generates a contract (pact) from successful tests
4. Publishes the pact to the Pact Broker server

## Test Structure

A typical Pact test consists of the following components using {@link Pact} annotations:

### RequestResponsePact Components

1. **Given (Provider State):**
    - Defines the required state of the Claims API
    - Example: "a claim exists" ensures a test claim is available
    - Can be reused across multiple test scenarios

2. **Upon Receiving:**
    - Describes the specific test scenario being verified

3. **Request Matching:**
    - Match Path: Defines the API endpoint to test
    - Match Header: Specifies required headers (e.g., authorization)
    - Method: Specifies the HTTP method to use

## Verification Process

1. Tests are executed against the mock server
2. Successful tests contribute to the pact definition
3. Generated pact is published to the Pact Broker
4. Claims API verifies compatibility against the published pact

## Pact Broker Integration

The Pact Broker serves as a central repository for contracts:

- Stores generated pacts from consumer tests
- Enables provider verification
- Maintains version history
- Facilitates contract evolution
