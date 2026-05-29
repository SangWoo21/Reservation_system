package com.project.reservation.exception;

public class SeatAlreadyReservedException extends RuntimeException {
    public SeatAlreadyReservedException(String seatCode) {
        super("이미 예약된 좌석입니다: " + seatCode);
    }
}