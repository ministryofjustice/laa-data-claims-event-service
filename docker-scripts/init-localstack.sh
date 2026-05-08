#!/bin/bash

echo "Initializing localstack SQS"

QUEUE_URL=$(awslocal sqs create-queue --queue-name claims-api-queue --attributes VisibilityTimeout=1200 --query 'QueueUrl' --output text)
QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-name QueueArn --query 'Attributes.QueueArn' --output text)


echo "Initializing localstack SNS"

TOPIC_ARN=$(awslocal sns create-topic --name claims-events --query 'TopicArn' --output text)


echo "Subscribing queue to topic"

awslocal sns subscribe --topic-arn $TOPIC_ARN --protocol sqs --notification-endpoint $QUEUE_ARN --attributes RawMessageDelivery=true