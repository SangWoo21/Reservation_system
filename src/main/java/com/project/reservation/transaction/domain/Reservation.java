package com.project.reservation.transaction.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    public static Reservation create(String userId, Seat seat) {
        Reservation r = new Reservation();
        r.userId = userId;
        r.seat = seat;
        r.reservedAt = LocalDateTime.now();
        return r;
    }
}