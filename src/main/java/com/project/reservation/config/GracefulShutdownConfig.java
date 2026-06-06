package com.project.reservation.config;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class GracefulShutdownConfig implements ApplicationListener<ContextClosedEvent> {

    private final RedisTemplate<String, String> redisTemplate;

    public GracefulShutdownConfig(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Scale In 시 Session Drop 방지 (슬라이드 주제 선정 이유 - Scale In 한계)
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        System.out.println("[SHUTDOWN] Graceful shutdown 시작 - 처리 중인 요청 완료 대기");

        try {
            Thread.sleep(5000); // 진행 중인 요청 완료 대기
            System.out.println("[SHUTDOWN] 요청 처리 완료 - 인스턴스 종료");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
