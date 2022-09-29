package org.springframework.sync.diffsync.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.sync.Patch;
import org.springframework.sync.PatchException;
import org.springframework.sync.diffsync.*;
import org.springframework.sync.diffsync.service.DiffSyncService;

import java.util.ArrayList;
import java.util.List;

@Service
public class DiffSyncServiceImpl implements DiffSyncService {

    private final ShadowStore shadowStore;
    private final Equivalency equivalency = new IdPropertyEquivalency();

    @Autowired
    public DiffSyncServiceImpl(ShadowStore shadowStore) {
        this.shadowStore = shadowStore;
    }

    @SuppressWarnings("unchecked")
    public <T> Patch applyAndDiff(Patch patch, Object target, PersistenceCallback<T> persistenceCallback) throws PatchException {
        DiffSync<T> sync = new DiffSync<>(shadowStore, persistenceCallback.getEntityType());
        T patched = sync.apply((T) target, patch);
        persistenceCallback.persistChange(patched);
        return sync.diff(patched);
    }

    public <T> Patch applyAndDiffAgainstList(Patch patch, List<T> target, PersistenceCallback<T> persistenceCallback) throws PatchException {
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
