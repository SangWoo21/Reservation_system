package com.project.reservation.waiting_redis.scheduler;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.reservation.common.RedisConstants;
import com.project.reservation.common.WaitingsApiResponse;
import com.project.reservation.waiting_redis.service.WaitingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class Queue1Scheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final WaitingService waitingService;

    @Value("${queue1.drain-size:10}")
    private int drainSize;

    @Scheduled(fixedDelay = 1000)
    public void drain() {
        Set<ZSetOperations.TypedTuple<String>> popped = redisTemplate.opsForZSet()
            .popMin(RedisConstants.QUEUE1, drainSize);

        if (popped == null || popped.isEmpty()) {
            return;
        }

        for (ZSetOperations.TypedTuple<String> tuple : popped) {
            String userId = tuple.getValue();
            if (userId == null) continue;

            try {
                WaitingsApiResponse<?> response = waitingService.addWaitingQueue(userId);
                if ("fail".equals(response.getStatus())) {
                    // Queue② 가득 참 → Queue①으로 복구
                    redisTemplate.opsForZSet().add(RedisConstants.QUEUE1, userId, System.currentTimeMillis());
                    log.warn("[Queue1] Queue② 가득 참, Queue①으로 복구: userId={}", userId);
                } else {
                    log.info("[Queue1→Queue2] userId={} 이동 완료", userId);
                }
            } catch (Exception e) {
                redisTemplate.opsForZSet().add(RedisConstants.QUEUE1, userId, System.currentTimeMillis());
                log.error("[Queue1] Queue② 이동 실패, 복구: userId={}", userId);
            }
        }
    }
}
