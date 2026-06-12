#!/bin/bash
set -e

echo "Waiting for SNS service and SQS service to be available"

echo "Waiting for SNS..."
until awslocal sns list-topics > /dev/null 2>&1; do
  echo "SNS is not ready yet"
  sleep 2
done
echo "Waiting for SQS..."
until awslocal sqs list-queues > /dev/null 2>&1; do
  echo "SQS is not ready yet"
  sleep 2
done

echo "Initializing localstack SNS"
TOPIC_ARN=$(awslocal sns create-topic --name claims-events --query 'TopicArn' --output text)

echo "Initializing localstack SQS"

CLAIMS_QUEUE_URL=$(awslocal sqs create-queue --queue-name claims-api-queue --attributes VisibilityTimeout=1200 --query 'QueueUrl' --output text)
CLAIMS_QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url $CLAIMS_QUEUE_URL --attribute-name QueueArn --query 'Attributes.QueueArn' --output text)

echo "Creating notify-queue"
NOTIFY_QUEUE_URL=$(awslocal sqs create-queue --queue-name notify-queue --attributes VisibilityTimeout=1200 --query 'QueueUrl' --output text)
NOTIFY_QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url $NOTIFY_QUEUE_URL --attribute-name QueueArn --query 'Attributes.QueueArn' --output text)

echo "Subscribing queue to topic"
awslocal sns subscribe \
  --topic-arn $TOPIC_ARN \
  --protocol sqs \
  --notification-endpoint $CLAIMS_QUEUE_ARN \
  --attributes '{"RawMessageDelivery":"true","FilterPolicy":"{\"SubmissionEventType\":[\"PARSE_BULK_SUBMISSION\",\"VALIDATE_SUBMISSION\"]}"}'
# ,FilterPolicy='{"SubmissionEventType":["PARSE_BULK_SUBMISSION", "VALIDATE_SUBMISSION"]}'

echo "Subscribing notify-queue to topic with filter policy"
awslocal sns subscribe \
  --topic-arn $TOPIC_ARN \
  --protocol sqs \
  --notification-endpoint $NOTIFY_QUEUE_ARN \
  --attributes '{"RawMessageDelivery":"true","FilterPolicy":"{\"SubmissionEventType\":[\"SUBMISSION_VALIDATION_SUCCEEDED\"]}"}'