package com.project.reservation.waiting_redis.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.project.reservation.common.RedisConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisRestoreService {

    private final RedisTemplate<String, String> redisTemplate;
    
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void restoreToWaitingQueue(String userId, Double score) {
        
        redisTemplate.opsForZSet().add(RedisConstants.WAITING_QUEUE, userId, score);
        log.info("{} 님 대기열 복구 성공", userId);
    }

    @Recover
    public void recoverFailed(Exception e, String userId, Double score) {
        log.error("[CRITICAL] {} 님 보구 최종 실패. score={}", userId, score);
    }
}
