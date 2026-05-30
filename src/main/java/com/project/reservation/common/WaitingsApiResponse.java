package com.project.reservation.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WaitingsApiResponse<T> {
    private String status;
    private String message;
    private T data;


    // 성공
    public static <T> WaitingsApiResponse<T> success(String message, T data) {
        return new WaitingsApiResponse<>("success", message, data);
    }

    public static <T> WaitingsApiResponse<T> success(String message) {
        return new WaitingsApiResponse<T>("success", message, null);
    }

    public static <T> WaitingsApiResponse<T> entered(String message) {
        return new WaitingsApiResponse<T>("entered", message, null);
    }

    // 실패
    public static <T> WaitingsApiResponse<T> fail(String message) {
        return new WaitingsApiResponse<T>("fail", message, null);
    }
}
