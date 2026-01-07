#!/bin/bash

# Script to submit bulk claim submissions to localstack SQS queue (0s mock the account ID)
docker exec event-service-localstack awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/claims-api-queue \
  --attribute-names All

docker exec event-service-localstack awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/claims-api-queue \
  --max-number-of-messages 1 \
  --visibility-timeout 0