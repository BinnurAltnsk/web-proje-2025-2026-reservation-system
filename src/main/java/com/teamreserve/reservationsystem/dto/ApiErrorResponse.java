package com.teamreserve.reservationsystem.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApiErrorResponse {
    private boolean success;
    private String message;
    private String error;
    private List<String> details;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ApiErrorResponse error(String message, String errorCode, List<String> details) {
        return ApiErrorResponse.builder()
                .success(false)
                .message(message)
                .error(errorCode)
                .details(details)
                .build();
    }
}