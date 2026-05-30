package com.project.reservation.waiting_redis.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.reservation.common.WaitingsApiResponse;
import com.project.reservation.waiting_redis.service.Queue1Service;
import com.project.reservation.waiting_redis.service.WaitingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WaitingController {

    private final WaitingService waitingService;
    private final Queue1Service queue1Service;

    // 대기열 진입
    @PostMapping("/api/waitings")
    public ResponseEntity<WaitingsApiResponse<?>> addWaiting(@RequestParam String userId) {
        if (queue1Service.isActive()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(queue1Service.enqueue(userId));
        }
        WaitingsApiResponse<?> response = waitingService.addWaitingQueue(userId);
        if (response.getStatus().equals("success")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    // 내 순번 확인
    @GetMapping("/api/waitings")
    public ResponseEntity<WaitingsApiResponse<?>> checkWaiting(@RequestParam String userId) {
        WaitingsApiResponse<?> response = waitingService.checkWaitingQueue(userId);
        if (response.getStatus().equals("success")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    // 상태 확인 (대기 중 / 입장 가능)
    @GetMapping("/api/waitings/status")
    public ResponseEntity<WaitingsApiResponse<?>> checkStatus(@RequestParam String userId) {
        WaitingsApiResponse<?> response = waitingService.checkUserStatus(userId);
        if ("fail".equals(response.getStatus())) {
            return ResponseEntity.status(404).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // Form 제출 (입장권 확인 후 예약 요청)
    @PostMapping("/api/waitings/confirm")
    public ResponseEntity<WaitingsApiResponse<?>> confirm(@RequestParam String userId) {
        WaitingsApiResponse<?> response = waitingService.confirmReservation(userId);
        if (response.getStatus().equals("success")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(403).body(response);
    }

    // 대기열 이탈
    @DeleteMapping("/api/waitings")
    public ResponseEntity<WaitingsApiResponse<?>> leaveWaiting(@RequestParam String userId) {
        WaitingsApiResponse<?> response = waitingService.leaveWaitingQueue(userId);
        if (response.getStatus().equals("success")) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body(response);
    }

    // 이벤트 종료
    @DeleteMapping("/api/waitings/close_event")
    public ResponseEntity<WaitingsApiResponse<?>> closeEvent(@RequestParam String eventId) {
        return ResponseEntity.ok(waitingService.closeEvent(eventId));
    }
}
