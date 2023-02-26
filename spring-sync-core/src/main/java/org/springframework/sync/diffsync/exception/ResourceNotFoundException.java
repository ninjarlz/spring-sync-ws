package org.springframework.sync.diffsync.exception;

public class ResourceNotFoundException extends Exception {
    private static final String REASON = "Resource with id '%s' was not found";

    public ResourceNotFoundException(String id) {
        super(String.format(REASON, id));
    }
}
