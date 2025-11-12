package com.gradproject.taskmanager.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    
    private boolean success;

    
    private T data;

    
    private String error;

    
    private Object details;

    
    @Builder.Default
    private Instant timestamp = Instant.now();

    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .build();
    }

    
    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .build();
    }

    
    public static <T> ApiResponse<T> error(String error, Object details) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .details(details)
                .build();
    }
}
