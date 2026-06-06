package com.project.reservation.config;

import com.project.reservation.waiting_redis.service.Queue1Service;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class GracefulShutdownConfig implements ApplicationListener<ContextClosedEvent> {

    private final RedisTemplate<String, String> redisTemplate;
    private final Queue1Service queue1Service;

    public GracefulShutdownConfig(RedisTemplate<String, String> redisTemplate,
                                  Queue1Service queue1Service) {
        this.redisTemplate = redisTemplate;
        this.queue1Service = queue1Service;
    }

    // Scale In 시 Session Drop 방지 (슬라이드 주제 선정 이유 - Scale In 한계)
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        System.out.println("[SHUTDOWN] Graceful shutdown 시작 - 처리 중인 요청 완료 대기");

        // Lambda가 terminate-hook CONTINUE 신호 전송 후 SIGTERM 수신
        // Queue1 비활성화 (scale-in 시작 신호)
        queue1Service.deactivate();

        try {
            Thread.sleep(5000); // 진행 중인 요청 완료 대기
            System.out.println("[SHUTDOWN] 요청 처리 완료 - 인스턴스 종료");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
