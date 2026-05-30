package com.project.reservation.waiting_redis.sqs;

import org.springframework.stereotype.Component;

import com.project.reservation.exception.SeatAlreadyReservedException;
import com.project.reservation.transaction.service.ReservationService;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Component
@Slf4j
public class SqsConsumer {

    private final ReservationService reservationService;

    @SqsListener("waiting-process-queue")
    public void consume(String message) {
        // message 형식: userId (좌석은 자동 배정)
        String userId = message.trim();

        log.info("[Consumer] userId={} 예약 처리 시작", userId);
        try {
            reservationService.reserveAutoAssign(userId);
            log.info("[Consumer] userId={} 예약 처리 완료", userId);
        } catch (SeatAlreadyReservedException e) {
            log.warn("[Consumer] 이미 예약된 좌석, 메시지 무시 userId={}", userId);
        }
        // 그 외 예외(DB 오류, 네트워크 등)는 rethrow → SQS가 재시도(maxReceiveCount=3) 후 DLQ
    }
}
