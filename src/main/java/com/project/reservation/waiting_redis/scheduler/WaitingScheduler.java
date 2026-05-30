package com.project.reservation.waiting_redis.scheduler;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.reservation.common.RedisConstants;
import com.project.reservation.waiting_redis.service.RedisRestoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class WaitingScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisRestoreService redisRestoreService;

    @Scheduled(fixedDelay = 1000)
    public void getWaitings() {
        Set<ZSetOperations.TypedTuple<String>> popped = redisTemplate.opsForZSet()
            .popMin(RedisConstants.WAITING_QUEUE, 5);

        if (popped == null || popped.isEmpty()) {
            return;
        }

        for (ZSetOperations.TypedTuple<String> tuple : popped) {
            String userId = tuple.getValue();
            Double score = tuple.getScore();

            if (userId == null) {
                log.warn("userId가 null인 튜플 발견, 스킵");
                continue;
            }

            try {
                // 입장권 발급 — 5분 안에 Form 제출해야 함
                String activeKey = "active_user:" + userId;
                Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(activeKey, "true", 5, TimeUnit.MINUTES);

                if (Boolean.FALSE.equals(isNew)) {
                    log.warn("{} 님 이미 입장권 보유 중, 스킵", userId);
                    continue;
                }

                log.info("{} 님 입장권 발급 완료 (5분 유효)", userId);
            } catch (Exception e) {
                log.error("{} 님 입장권 발급 실패, 대기열 복구: {}", userId, e.getMessage());
                redisRestoreService.restoreToWaitingQueue(userId, score);
            }
        }
    }
}
