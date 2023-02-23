package org.springframework.sync.diffsync.exception;

public class PersistenceCallbackNotFoundException extends Exception {

    private static final String REASON = "Persistence callback for resource with name '%s' was not found";

    public PersistenceCallbackNotFoundException(String resourceName) {
        super(String.format(REASON, resourceName));
    }
}
