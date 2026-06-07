package com.project.reservation.waiting_redis.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.project.reservation.common.RedisConstants;
import com.project.reservation.common.WaitingsApiResponse;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SqsTemplate sqsTemplate;

    @Value("${cloud.aws.sqs.queue-url}")
    private String queueUrl;

    @Value("${hmac.secret.key}")
    private String hmacSecretKey;

    private static final long MAX_CAPACITY = 1000L;

    public WaitingsApiResponse<?> addWaitingQueue(String userId) {
        String script =
            "if tonumber(redis.call('ZCARD', KEYS[1])) >= tonumber(ARGV[2]) then" +
            "    return 'FULL'" +
            "else" +
            "    redis.call('ZADD', KEYS[1], 'NX', ARGV[1], KEYS[2])" +
            "    return 'SUCCESS'" +
            "end";

        String result = redisTemplate.execute(
            new DefaultRedisScript<>(script, String.class),
            List.of(RedisConstants.WAITING_QUEUE, userId),
            String.valueOf(System.currentTimeMillis()),
            String.valueOf(MAX_CAPACITY)
        );

        if ("FULL".equals(result)) {
            return WaitingsApiResponse.fail("대기열이 꽉 찼습니다.");
        }

        Long rank = redisTemplate.opsForZSet().rank(RedisConstants.WAITING_QUEUE, userId);
        String token = generateHmacToken(userId);
        return WaitingsApiResponse.success("대기열 진입 성공", Map.of("rank", rank + 1, "userId", userId, "token", token));
    }

    public WaitingsApiResponse<?> checkWaitingQueue(String userId) {
        Long rank = redisTemplate.opsForZSet().rank(RedisConstants.WAITING_QUEUE, userId);

        if (rank == null) {
            return WaitingsApiResponse.fail("대기열에 없습니다.");
        }

        return WaitingsApiResponse.success((rank + 1) + "번째 대기중", Map.of("rank", rank + 1, "userId", userId));
    }

    public WaitingsApiResponse<?> checkUserStatus(String userId) {
        String activeKey = "active_user:" + userId;

        // 입장권 보유 → Form 작성 가능
        if (Boolean.TRUE.equals(redisTemplate.hasKey(activeKey))) {
            return WaitingsApiResponse.entered("Form 작성 페이지로 입장 가능합니다.");
        }

        // 아직 Queue②에서 대기 중
        Long rank = redisTemplate.opsForZSet().rank(RedisConstants.WAITING_QUEUE, userId);
        if (rank != null) {
            return WaitingsApiResponse.success((rank + 1) + "번째 대기중", Map.of("rank", rank + 1, "userId", userId));
        }

        return WaitingsApiResponse.fail("대기열에 없습니다.");
    }

    public WaitingsApiResponse<?> confirmReservation(String userId) {
        String activeKey = "active_user:" + userId;

        // 입장권 확인 + 삭제를 원자적으로 처리 (TOCTOU 방지)
        String atomicCheckAndDelete =
            "if redis.call('EXISTS', KEYS[1]) == 1 then" +
            "    redis.call('DEL', KEYS[1])" +
            "    return 1" +
            "else" +
            "    return 0" +
            "end";

        Long deleted = redisTemplate.execute(
            new DefaultRedisScript<>(atomicCheckAndDelete, Long.class),
            List.of(activeKey)
        );

        if (deleted == null || deleted == 0L) {
            return WaitingsApiResponse.fail("입장 권한이 없거나 시간이 초과되었습니다.");
        }

        // SQS 발행 → Consumer → 자동 배정
        sqsTemplate.send(queueUrl, userId);
        
        log.info("{} 님 Form 제출 완료, 예약 처리 중", userId);
        return WaitingsApiResponse.success("예약 요청이 완료되었습니다.");
    }

    public WaitingsApiResponse<?> leaveWaitingQueue(String userId) {
        Long removed = redisTemplate.opsForZSet().remove(RedisConstants.WAITING_QUEUE, userId);
        String activeKey = "active_user:" + userId;
        Boolean hasActiveKey = redisTemplate.delete(activeKey);

        if ((removed == null || removed == 0L) && !Boolean.TRUE.equals(hasActiveKey)) {
            return WaitingsApiResponse.fail("대기열에 없습니다.");
        }

        log.info("{} 님 이탈 처리 완료", userId);
        return WaitingsApiResponse.success(userId + "님이 대기열에서 이탈했습니다.");
    }

    public WaitingsApiResponse<?> closeEvent(String eventId) {
        redisTemplate.delete(RedisConstants.WAITING_QUEUE);
        log.info("[이벤트 종료] eventId={} 대기열 삭제 완료", eventId);
        return WaitingsApiResponse.success("이벤트가 종료되었습니다.");
    }

    // HMAC-SHA256 토큰 생성 (슬라이드 ⑤번 - 대기열 토큰 위조 방어)
    private String generateHmacToken(String userId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(hmacSecretKey.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            return Base64.getEncoder().encodeToString(mac.doFinal(userId.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Token generation failed", e);
        }
    }
}
