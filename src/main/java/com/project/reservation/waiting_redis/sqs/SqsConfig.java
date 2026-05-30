package com.project.reservation.waiting_redis.sqs;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

// LocalStack endpoint가 설정된 경우에만 활성화 (로컬 개발용)
// AWS 배포 시: 이 클래스 전체 비활성 → Spring Cloud AWS가 EC2 IAM Role로 자동 구성
@Configuration
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.endpoint")
public class SqsConfig {

    @Bean
    public SqsAsyncClient sqsAsyncClient(
            @Value("${spring.cloud.aws.sqs.endpoint}") String sqsEndpoint,
            @Value("${spring.cloud.aws.region.static}") String region) {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(sqsEndpoint))
                .region(Region.of(region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")
                    )
                )
                .build();
    }

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.newTemplate(sqsAsyncClient);
    }
}
