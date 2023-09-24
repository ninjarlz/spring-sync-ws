package org.springframework.sync.diffsync.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.sync.Patch;
import org.springframework.sync.diffsync.DiffSync;
import org.springframework.sync.diffsync.Equivalency;
import org.springframework.sync.diffsync.PersistenceCallback;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.exception.PersistenceCallbackNotFoundException;
import org.springframework.sync.diffsync.exception.ResourceNotFoundException;
import org.springframework.sync.diffsync.service.DiffSyncService;
import org.springframework.sync.diffsync.shadowstore.ShadowStore;
import org.springframework.sync.exception.PatchException;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackOn = Exception.class)
public class DiffSyncServiceImpl implements DiffSyncService {

    private final PersistenceCallbackRegistry callbackRegistry;
    private final Equivalency equivalency;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Patch patch(ShadowStore shadowStore, String resource, Patch patch) throws PersistenceCallbackNotFoundException, PatchException {
        PersistenceCallback<?> persistenceCallback = callbackRegistry.findPersistenceCallback(resource);
        return applyAndDiffAgainstList(shadowStore, patch, (List) persistenceCallback.findAll(), persistenceCallback);
    }

    @Override
    public Patch patch(ShadowStore shadowStore, String resource, String id, Patch patch) throws PersistenceCallbackNotFoundException, PatchException, ResourceNotFoundException {
        PersistenceCallback<?> persistenceCallback = callbackRegistry.findPersistenceCallback(resource);
        Object findOne = persistenceCallback.findOne(id);
        return applyAndDiff(shadowStore, patch, findOne, persistenceCallback);
    }

    @SuppressWarnings("unchecked")
    private <T> Patch applyAndDiff(ShadowStore shadowStore, Patch patch, Object target, PersistenceCallback<T> persistenceCallback) throws PatchException {
        DiffSync<T> sync = new DiffSync<>(shadowStore, persistenceCallback.getEntityType());
        T patched = sync.apply((T) target, patch);
        persistenceCallback.persistChange(patched);
        return sync.diff(patched);
    }

    private <T> Patch applyAndDiffAgainstList(ShadowStore shadowStore, Patch patch, List<T> target, PersistenceCallback<T> persistenceCallback) throws PatchException {
        DiffSync<T> sync = new DiffSync<>(shadowStore, persistenceCallback.getEntityType());

        List<T> patched = sync.apply(target, patch);

        List<T> itemsToSave = new ArrayList<>(patched);
        itemsToSave.removeAll(target);

        // Determine which items should be deleted.
        // Make a shallow copy of the target, remove items that are equivalent to items in the working copy.
        // Equivalent is not the same as equals. It means "this is the same resource, even if it has changed".
        // It usually means "are the id properties equals".
        List<T> itemsToDelete = new ArrayList<>(target);
        target.forEach(candidate -> {
            for (T item : patched) {
                if (equivalency.isEquivalent(candidate, item)) {
                    itemsToDelete.remove(candidate);
                    break;
                }
            }
        });
        persistenceCallback.persistChanges(itemsToSave, itemsToDelete);

        return sync.diff(patched);
    }
}
