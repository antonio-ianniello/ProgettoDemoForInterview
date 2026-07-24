package com.example.usermanagement.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " with identifier '" + identifier + "' was not found");
    }
}
