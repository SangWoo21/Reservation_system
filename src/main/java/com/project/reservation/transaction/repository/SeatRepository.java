package com.project.reservation.transaction.repository;

import com.project.reservation.transaction.domain.Seat;
import com.project.reservation.transaction.domain.SeatStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 특정 좌석 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query("SELECT s FROM Seat s WHERE s.id = :seatId")
    Optional<Seat> findByIdWithLock(@Param("seatId") Long seatId);

    // 자동 배정용 — 빈 좌석 1개만 비관적 락으로 조회 (LIMIT 1 FOR UPDATE)
    // nativeQuery는 @Lock 미지원 -> FOR UPDATE 직접 명시
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Query(value = "SELECT * FROM seats WHERE status = 'AVAILABLE' ORDER BY id ASC LIMIT 1 FOR UPDATE", nativeQuery = true)
    Optional<Seat> findFirstAvailableWithLock();

    List<Seat> findByStatus(SeatStatus status);
}