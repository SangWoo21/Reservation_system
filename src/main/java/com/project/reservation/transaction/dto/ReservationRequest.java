package com.project.reservation.transaction.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationRequest {
    private String userId;
    private Long seatId;
}