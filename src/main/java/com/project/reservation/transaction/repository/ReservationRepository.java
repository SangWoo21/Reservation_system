package com.project.reservation.transaction.repository;

import com.project.reservation.transaction.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByUserId(String userId);
}