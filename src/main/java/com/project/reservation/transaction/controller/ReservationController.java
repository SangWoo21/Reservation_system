package com.project.reservation.transaction.controller;

import com.project.reservation.transaction.dto.ReservationRequest;
import com.project.reservation.transaction.dto.ReservationResponse;
import com.project.reservation.transaction.dto.SeatResponse;
import com.project.reservation.transaction.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // 예약 요청
    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(
            @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService
                .reserve(request.getUserId(), request.getSeatId());
        return ResponseEntity.ok(response);
    }

    // 예약 가능 좌석 조회
    @GetMapping("/seats/available")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats() {
        return ResponseEntity.ok(reservationService.getAvailableSeats());
    }
}