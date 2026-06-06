package com.project.reservation.waiting_redis.sqs;

import org.springframework.stereotype.Component;

import com.project.reservation.waiting_redis.service.Queue1Service;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LifecycleHookConsumer {

    private final Queue1Service queue1Service;

    // ASG Lifecycle Hook → SQS(lifecycle-hook-queue) → 여기서 소비
    // TERMINATING 처리는 Lambda(lifecycle-hook-terminate-queue)에서 담당
    @SqsListener("lifecycle-hook-queue")
    public void consume(String message) {
        log.info("[LifecycleHook] 수신: {}", message);

        if (message.contains("autoscaling:EC2_INSTANCE_LAUNCHING")) {
            if (queue1Service.isAutoEnabled()) {
                queue1Service.activate();
            } else {
                log.info("[LifecycleHook] auto-enabled OFF — Queue1 활성화 건너뜀 (Warm Pool 페이즈)");
            }
        }
    }
}
