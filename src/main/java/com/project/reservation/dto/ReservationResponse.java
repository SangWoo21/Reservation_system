package com.project.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationResponse {
    private boolean success;
    private Long reservationId;
    private String seatCode;
    private String message;

    public static ReservationResponse success(Long reservationId, String seatCode) {
        return new ReservationResponse(true, reservationId, seatCode, "예약 성공");
    }
}