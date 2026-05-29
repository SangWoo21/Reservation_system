package com.project.reservation.service;

import com.project.reservation.domain.Reservation;
import com.project.reservation.domain.Seat;
import com.project.reservation.domain.SeatStatus;
import com.project.reservation.dto.ReservationResponse;
import com.project.reservation.dto.SeatResponse;
import com.project.reservation.repository.ReservationRepository;
import com.project.reservation.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(timeout = 5)
    public ReservationResponse reserve(String userId, Long seatId) {

        // 1. 비관적 락으로 좌석 조회 (SELECT FOR UPDATE)
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 좌석: " + seatId));

        // 2. 예약 처리 (이미 예약된 경우 예외 발생)
        seat.reserve();

        // 3. 예약 저장
        Reservation reservation = Reservation.create(userId, seat);
        reservationRepository.save(reservation);

        log.info("예약 성공 - userId: {}, seatCode: {}", userId, seat.getSeatCode());
        return ReservationResponse.success(reservation.getId(), seat.getSeatCode());
    }

    // 예약 가능 좌석 목록
    @Transactional(readOnly = true)
    public List<SeatResponse> getAvailableSeats() {
        return seatRepository.findByStatus(SeatStatus.AVAILABLE)
                .stream()
                .map(SeatResponse::from)
                .collect(Collectors.toList());
    }
}