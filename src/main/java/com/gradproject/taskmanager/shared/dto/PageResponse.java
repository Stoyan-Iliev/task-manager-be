package com.gradproject.taskmanager.shared.dto;

import java.util.List;


public record PageResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
) {
    
    public boolean isFirst() {
        return pageNumber == 0;
    }

    
    public boolean isLast() {
        return pageNumber >= totalPages - 1;
    }

    
    public boolean hasNext() {
        return pageNumber < totalPages - 1;
    }

    
    public boolean hasPrevious() {
        return pageNumber > 0;
    }

    
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }
}
