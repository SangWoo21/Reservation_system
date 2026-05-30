package com.project.reservation.waiting_redis.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.project.reservation.common.RedisConstants;
import com.project.reservation.common.WaitingsApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class Queue1Service {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${queue1.ttl-minutes:3}")
    private long ttlMinutes;

    public boolean isActive() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisConstants.QUEUE1_ACTIVE_KEY));
    }

    public void activate() {
        redisTemplate.opsForValue().set(RedisConstants.QUEUE1_ACTIVE_KEY, "true", ttlMinutes, TimeUnit.MINUTES);
        log.info("[Queue1] 활성화 — TTL {}분 (scale-out 감지)", ttlMinutes);
    }

    public void deactivate() {
        redisTemplate.delete(RedisConstants.QUEUE1_ACTIVE_KEY);
        log.info("[Queue1] 비활성화 — 이후 요청은 Queue②로 직행");
    }

    public WaitingsApiResponse<?> enqueue(String userId) {
        redisTemplate.opsForZSet().add(RedisConstants.QUEUE1, userId, System.currentTimeMillis());
        Long rank = redisTemplate.opsForZSet().rank(RedisConstants.QUEUE1, userId);
        log.info("[Queue1] 진입 userId={}", userId);
        return WaitingsApiResponse.success("요청이 접수되었습니다.", Map.of("position", rank == null ? 1 : rank + 1));
    }
}
