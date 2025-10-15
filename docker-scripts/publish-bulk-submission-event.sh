#!/bin/bash

# Script to publish a bulk claim submission event to localstack SQS queue (0s mock the account ID)
docker exec event-service-localstack awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/claims-api-queue \
  --message-body '{"bulk_submission_id":"e18ea33c-ab5c-4710-8906-5dad420fa3f7", "submission_ids": ["63d759fc-089f-4e6b-a250-08caaea6f101", "5fdfbb22-ac6c-4220-ab3b-050ff7d27dd1"]}' \
  --message-attributes '{"SubmissionEventType": {"StringValue": "PARSE_BULK_SUBMISSION", "DataType": "String"}}' \
  --endpoint-url=http://localhost:4566
