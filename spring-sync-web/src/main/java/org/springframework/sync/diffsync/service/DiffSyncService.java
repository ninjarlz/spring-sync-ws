package org.springframework.sync.diffsync.service;

import org.springframework.sync.Patch;
import org.springframework.sync.diffsync.exception.PersistenceCallbackNotFoundException;
import org.springframework.sync.diffsync.exception.ResourceNotFoundException;
import org.springframework.sync.exception.PatchException;

public interface DiffSyncService {
    Patch patch(String resource, Patch patch) throws PersistenceCallbackNotFoundException, PatchException;
    Patch patch(String resource, String id, Patch patch) throws PersistenceCallbackNotFoundException, PatchException, ResourceNotFoundException;
}
