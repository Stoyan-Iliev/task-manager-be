package com.gradproject.taskmanager.shared.exception;


public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceType, String field, Object value) {
        super(String.format("%s with %s '%s' already exists", resourceType, field, value));
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
