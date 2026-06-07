package com.project.reservation.transaction.repository;

import com.project.reservation.transaction.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByUserId(String userId);
    Optional<Reservation> findByUserId(String userId);
}