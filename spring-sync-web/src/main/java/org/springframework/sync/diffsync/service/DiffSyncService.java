package org.springframework.sync.diffsync.service;

import org.springframework.sync.Patch;
import org.springframework.sync.PatchException;
import org.springframework.sync.diffsync.PersistenceCallback;

import java.util.List;

public interface DiffSyncService {
    <T> Patch applyAndDiff(Patch patch, Object target, PersistenceCallback<T> persistenceCallback) throws PatchException;
    <T>  Patch applyAndDiffAgainstList(Patch patch, List<T> target, PersistenceCallback<T> persistenceCallback) throws PatchException;
}
