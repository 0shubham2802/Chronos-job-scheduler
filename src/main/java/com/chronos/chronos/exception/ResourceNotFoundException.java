package com.chronos.chronos.exception;

// Custom exception for when a resource doesn't exist
// or doesn't belong to the current user
// Extends RuntimeException so we don't need to declare it in method signatures
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Convenience factory method — reads naturally in code
    // Usage: throw ResourceNotFoundException.job(jobId)
    public static ResourceNotFoundException job(Object id) {
        return new ResourceNotFoundException(
                "Job not found with id: " + id
        );
    }
}
