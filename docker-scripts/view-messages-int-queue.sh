#!/bin/bash

# Script to submit bulk claim submissions to localstack SQS queue
docker exec event-service-localstack awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/bulk-claims-queue \
  --attribute-names All
