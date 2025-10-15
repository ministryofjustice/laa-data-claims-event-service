#!/bin/bash

# Script to submit bulk claim submissions to localstack SQS queue (0s mock the account ID)
docker exec claims-data-localstack-1 awslocal sqs purge-queue \
  --queue-url http://localhost:4566/000000000000/claims-api-queue