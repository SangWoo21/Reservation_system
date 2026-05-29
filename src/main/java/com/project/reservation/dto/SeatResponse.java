package com.project.reservation.dto;

import com.project.reservation.domain.Seat;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatResponse {
    private Long seatId;
    private String seatCode;
    private String status;

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getSeatCode(),
                seat.getStatus().name()
        );
    }
}