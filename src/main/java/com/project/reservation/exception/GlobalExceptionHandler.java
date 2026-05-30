package com.project.reservation.exception;

import com.project.reservation.common.WaitingsApiResponse;
import com.project.reservation.transaction.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 이미 예약된 좌석
    @ExceptionHandler(SeatAlreadyReservedException.class)
    public ResponseEntity<ErrorResponse> handleSeatReserved(SeatAlreadyReservedException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SEAT_RESERVED", e.getMessage()));
    }

    // 비관적 락 대기 타임아웃
    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<ErrorResponse> handleLockTimeout(JpaSystemException e) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("LOCK_TIMEOUT", "잠시 후 다시 시도해주세요"));
    }

    // Redis 연결 실패
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<WaitingsApiResponse<?>> handleRedisConnectionFailure(
            RedisConnectionFailureException e) {
        log.error("[Redis 연결 실패] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(WaitingsApiResponse.fail("서버가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요."));
    }

    // 잘못된 파라미터
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<WaitingsApiResponse<?>> handleIllegalArgument(
            IllegalArgumentException e) {
        log.error("[잘못된 요청] {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(WaitingsApiResponse.fail(e.getMessage()));
    }

    // 그 외 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<WaitingsApiResponse<?>> handleException(Exception e) {
        log.error("[서버 오류] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WaitingsApiResponse.fail("서버 오류가 발생했습니다."));
    }
}