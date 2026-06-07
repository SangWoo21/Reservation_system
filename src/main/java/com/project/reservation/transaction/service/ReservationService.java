package com.project.reservation.transaction.service;

import com.project.reservation.transaction.domain.Reservation;
import com.project.reservation.transaction.domain.Seat;
import com.project.reservation.transaction.domain.SeatStatus;
import com.project.reservation.transaction.dto.ReservationResponse;
import com.project.reservation.transaction.dto.SeatResponse;
import com.project.reservation.transaction.repository.ReservationRepository;
import com.project.reservation.transaction.repository.SeatRepository;
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
    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    @Transactional(timeout = 5)
    public ReservationResponse reserve(String userId, Long seatId) {
        // ... (existing code unchanged for now, but will use the same notification logic if needed)
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 좌석: " + seatId));
        seat.reserve();
        Reservation reservation = Reservation.create(userId, seat);
        reservationRepository.save(reservation);

        log.info("예약 성공 - userId: {}, seatCode: {}", userId, seat.getSeatCode());
        return ReservationResponse.success(reservation.getId(), seat.getSeatCode());
    }

    // 대기열 방식 — 빈 좌석 자동 배정 (1개 행만 락)
    @Transactional(timeout = 5)
    public ReservationResponse reserveAutoAssign(String userId) {
        if (reservationRepository.existsByUserId(userId)) {
            return ReservationResponse.success(0L, "ALREADY_RESERVED");
        }
        Seat seat = seatRepository.findFirstAvailableWithLock()
                .orElseThrow(() -> new IllegalArgumentException("예약 가능한 좌석이 없습니다."));
        seat.reserve();
        Reservation reservation = Reservation.create(userId, seat);
        reservationRepository.save(reservation);
        
        // Redis에 알림 저장 (10분 유효)
        String noticeKey = com.project.reservation.common.RedisConstants.RESERVATION_NOTIFICATION + userId;
        redisTemplate.opsForValue().set(noticeKey, seat.getSeatCode(), 10, java.util.concurrent.TimeUnit.MINUTES);

        log.info("자동 배정 예약 성공 - userId: {}, seatCode: {}", userId, seat.getSeatCode());
        return ReservationResponse.success(reservation.getId(), seat.getSeatCode());
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationStatus(String userId) {
        // 1. Redis 알림 확인 (빠른 응답)
        String noticeKey = com.project.reservation.common.RedisConstants.RESERVATION_NOTIFICATION + userId;
        String seatCode = redisTemplate.opsForValue().get(noticeKey);
        
        if (seatCode != null) {
            return ReservationResponse.success(null, seatCode);
        }

        // 2. DB 확인 (Redis 만료 등의 경우 대비)
        return reservationRepository.findByUserId(userId)
                .map(r -> ReservationResponse.success(r.getId(), r.getSeat().getSeatCode()))
                .orElse(null);
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