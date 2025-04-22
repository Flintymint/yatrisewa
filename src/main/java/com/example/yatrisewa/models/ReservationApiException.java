package com.example.yatrisewa.models;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class ReservationApiException extends RuntimeException {
    @Getter
    private final HttpStatus status;
    private final String message;

    public ReservationApiException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
