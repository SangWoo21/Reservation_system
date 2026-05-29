package com.project.reservation.exception;

import com.project.reservation.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    // 존재하지 않는 좌석
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
    }
}