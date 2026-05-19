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

QUEUE_URL=$(awslocal sqs create-queue --queue-name claims-api-queue --attributes VisibilityTimeout=1200 --query 'QueueUrl' --output text)
QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-name QueueArn --query 'Attributes.QueueArn' --output text)

echo "Subscribing queue to topic"

awslocal sns subscribe --topic-arn $TOPIC_ARN --protocol sqs --notification-endpoint $QUEUE_ARN --attributes RawMessageDelivery=true