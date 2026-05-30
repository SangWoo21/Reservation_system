package com.project.reservation;

import com.project.reservation.transaction.domain.Seat;
import com.project.reservation.transaction.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SeatRepository seatRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 좌석이 없을 때만 초기 데이터 삽입
        if (seatRepository.count() == 0) {
            for (int row = 1; row <= 5; row++) {
                for (int col = 1; col <= 10; col++) {
                    seatRepository.save(
                            Seat.createAvailable(row + "-" + col)
                    );
                }
            }
            System.out.println("좌석 50개 초기화 완료");
        }
    }
}