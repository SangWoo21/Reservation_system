#!/bin/sh

echo "LocalStack 대기중..."
until aws --endpoint-url=http://localstack:4566 sqs list-queues --region us-east-1 > /dev/null 2>&1; do
  sleep 2
done

echo "DLQ 생성..."
aws --endpoint-url=http://localstack:4566 sqs create-queue --queue-name waiting-process-dlq --region us-east-1

DLQ_ARN=$(aws --endpoint-url=http://localstack:4566 sqs get-queue-attributes \
  --queue-url http://localstack:4566/000000000000/waiting-process-dlq \그
  --attribute-names QueueArn \
  --region us-east-1 \
  --query 'Attributes.QueueArn' \
  --output text)

echo "메인 큐 생성... DLQ ARN: $DLQ_ARN"
aws --endpoint-url=http://localstack:4566 sqs create-queue \
  --queue-name waiting-process-queue \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}" \
  --region us-east-1

echo "메인 큐 생성 완료"

echo "Lifecycle Hook 큐 생성..."
aws --endpoint-url=http://localstack:4566 sqs create-queue \
  --queue-name lifecycle-hook-queue \
  --region us-east-1

echo "모든 큐 생성 완료"