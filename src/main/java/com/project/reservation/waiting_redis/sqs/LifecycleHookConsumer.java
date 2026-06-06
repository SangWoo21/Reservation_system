package com.project.reservation.waiting_redis.sqs;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.reservation.service.LifecycleHookService;
import com.project.reservation.waiting_redis.service.Queue1Service;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LifecycleHookConsumer {

    private final Queue1Service queue1Service;
    private final LifecycleHookService lifecycleHookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ASG Lifecycle Hook → SNS → SQS(lifecycle-hook-queue) → 여기서 소비
    // 메시지 형식: ASG가 보내는 JSON (LifecycleTransition 필드 포함)
    @SqsListener("lifecycle-hook-queue")
    public void consume(String message) {
        log.info("[LifecycleHook] 수신: {}", message);

        if (message.contains("autoscaling:EC2_INSTANCE_LAUNCHING")) {
            if (queue1Service.isAutoEnabled()) {
                queue1Service.activate();
            } else {
                log.info("[LifecycleHook] auto-enabled OFF — Queue1 활성화 건너뜀 (Warm Pool 페이즈)");
            }
        } else if (message.contains("autoscaling:EC2_INSTANCE_TERMINATING")) {
            // scale-in 시작 → Queue① 비활성화
            queue1Service.deactivate();

            // ASG에 terminate-hook CONTINUE 신호 전송
            try {
                JsonNode json = objectMapper.readTree(message);
                String instanceId = json.get("EC2InstanceId").asText();
                lifecycleHookService.completeTermination(instanceId);
            } catch (Exception e) {
                log.error("[LifecycleHook] instanceId 파싱 실패: {}", e.getMessage());
            }
        }
    }
}
