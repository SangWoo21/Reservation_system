package com.project.reservation.transaction.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.project.reservation.exception.SeatAlreadyReservedException;

@Entity
@Table(name = "seats")
@Getter
@NoArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String seatCode;   // ex) "A-1", "A-2"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    // 테스트용 초기 데이터 생성 메서드
    public static Seat createAvailable(String seatCode) {
        Seat seat = new Seat();
        seat.seatCode = seatCode;
        seat.status = SeatStatus.AVAILABLE;
        return seat;
    }

    public void reserve() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new SeatAlreadyReservedException(seatCode);
        }
        this.status = SeatStatus.RESERVED;
    }
}