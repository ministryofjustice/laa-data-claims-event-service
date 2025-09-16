#!/bin/bash

# Script to publish a submission validation event to localstack SQS queue (0s mock the account ID)
docker exec event-service-localstack awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/claims-api-queue \
  --message-body '{"submission_id":"0561d67b-30ed-412e-8231-f6296a53538d"}' \
  --message-attributes '{"SubmissionEventType": {"StringValue": "VALIDATE_SUBMISSION", "DataType": "string"}}' \
  --endpoint-url=http://localhost:4566
