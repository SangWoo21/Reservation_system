package com.project.reservation.waiting_redis.scheduler;

import java.util.ArrayList;
import java.util.List;
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

    private static final long MAX_QUEUE2_CAPACITY = 1000L;

    @Scheduled(fixedDelay = 500) // 0.5초마다 실행하여 더 촘촘하게 제어
    public void drain() {
        Long q1Size = redisTemplate.opsForZSet().size(RedisConstants.QUEUE1);
        if (q1Size == null || q1Size == 0) return;

        Long q2Size = redisTemplate.opsForZSet().size(RedisConstants.WAITING_QUEUE);
        long q2SizeVal = q2Size != null ? q2Size : 0;
        long available = MAX_QUEUE2_CAPACITY - q2SizeVal;

        if (available <= 0) {
            log.debug("[Queue1] Queue② 가득 참, 드레인 스킵 (q2Size={})", q2SizeVal);
            return;
        }

        // 1. 동적 드레인 속도(Refill Rate) 계산
        int refillRate = calculateRefillRate(q1Size, q2SizeVal);
        
        // 2. 토큰 버킷에서 토큰 획득 (Global Rate Limit)
        int toDrain = getTokens(drainSize, refillRate, 20); // Bucket Size 20으로 Burst 허용

        if (toDrain <= 0) return;

        toDrain = (int) Math.min(toDrain, available);
        
        Set<ZSetOperations.TypedTuple<String>> popped = redisTemplate.opsForZSet()
            .popMin(RedisConstants.QUEUE1, toDrain);

        if (popped == null || popped.isEmpty()) {
            return;
        }

        log.info("[Queue1] 드레인 실행: rate={}토큰/s, size={}", refillRate, popped.size());

        List<ZSetOperations.TypedTuple<String>> toRestore = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> tuple : popped) {
            String userId = tuple.getValue();
            if (userId == null) continue;

            try {
                WaitingsApiResponse<?> response = waitingService.addWaitingQueue(userId);
                if ("fail".equals(response.getStatus())) {
                    toRestore.add(tuple);
                    log.warn("[Queue1] Queue② 가득 참, 순서 유지 복구: userId={}", userId);
                } else {
                    log.debug("[Queue1→Queue2] userId={} 이동 완료", userId);
                }
            } catch (Exception e) {
                toRestore.add(tuple);
                log.error("[Queue1] Queue② 이동 실패, 순서 유지 복구: userId={}", userId);
            }
        }

        if (!toRestore.isEmpty()) {
            redisTemplate.opsForZSet().add(RedisConstants.QUEUE1, toRestore.stream()
                .collect(java.util.stream.Collectors.toSet()));
        }
    }

    private int calculateRefillRate(long q1Size, long q2Size) {
        // 기본 목표 속도: 5명/sec (Queue2 처리 용량)
        int rate = 5;
        
        // Queue2 여유 공간에 따른 조절 (Gradual Drain)
        if (q2Size < 300) {
            rate = 10; // 여유 많으면 빠르게 채움
        } else if (q2Size > 800) {
            rate = 2;  // 거의 다 차면 감속
        }
        
        // Queue1 규모에 따른 동적 가속
        if (q1Size > 5000) {
            rate = (int) (rate * 1.5);
        } else if (q1Size > 2000) {
            rate = (int) (rate * 1.2);
        }
        
        return Math.min(rate, 20); // 최대 20명/sec 제한
    }

    private int getTokens(int requested, int refillRate, int maxTokens) {
        String script =
            "local key = KEYS[1] " +
            "local max_tokens = tonumber(ARGV[1]) " +
            "local refill_rate = tonumber(ARGV[2]) " +
            "local now = tonumber(ARGV[3]) " +
            "local requested = tonumber(ARGV[4]) " +
            "local bucket = redis.call('HMGET', key, 'tokens', 'last') " +
            "local tokens = tonumber(bucket[1] or max_tokens) " +
            "local last = tonumber(bucket[2] or now) " +
            "local delta = math.max(0, now - last) " +
            "tokens = math.min(max_tokens, tokens + (delta * refill_rate / 1000)) " +
            "local granted = math.floor(math.min(tokens, requested)) " +
            "tokens = tokens - granted " +
            "redis.call('HMSET', key, 'tokens', tokens, 'last', now) " +
            "return granted";

        Long granted = redisTemplate.execute(
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
            java.util.Collections.singletonList(RedisConstants.QUEUE1_TOKEN_BUCKET),
            String.valueOf(maxTokens),
            String.valueOf(refillRate),
            String.valueOf(System.currentTimeMillis()),
            String.valueOf(requested)
        );
        return granted != null ? granted.intValue() : 0;
    }
}
