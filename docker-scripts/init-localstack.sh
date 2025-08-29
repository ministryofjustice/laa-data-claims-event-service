#!/bin/bash

echo "Initializing localstack SQS"

awslocal sqs create-queue --queue-name bulk-claims-queue