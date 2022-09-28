/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.sync.diffsync.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.sync.Patch;
import org.springframework.sync.PatchException;
import org.springframework.sync.diffsync.DiffSync;
import org.springframework.sync.diffsync.Equivalency;
import org.springframework.sync.diffsync.IdPropertyEquivalency;
import org.springframework.sync.diffsync.PersistenceCallback;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.ShadowStore;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle PATCH requests an apply them to resources using {@link DiffSync}.
 * @author Craig Walls
 */
@RestController
public class DiffSyncController {
	
	private final ShadowStore shadowStore;

	private final PersistenceCallbackRegistry callbackRegistry;
	
	private final Equivalency equivalency = new IdPropertyEquivalency();

	@Autowired
	public DiffSyncController(PersistenceCallbackRegistry callbackRegistry, ShadowStore shadowStore) {
		this.callbackRegistry = callbackRegistry;
		this.shadowStore = shadowStore;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping(
			value="${spring.diffsync.path:}/{resource}",
			method=RequestMethod.PATCH)
	public Patch patch(@PathVariable("resource") String resource, @RequestBody Patch patch) throws PatchException {
		PersistenceCallback<?> persistenceCallback = callbackRegistry.findPersistenceCallback(resource);		
		return applyAndDiffAgainstList(patch, (List) persistenceCallback.findAll(), persistenceCallback);
	}

	@RequestMapping(
			value="${spring.diffsync.path:}/{resource}/{id}",
			method=RequestMethod.PATCH)
	public Patch patch(@PathVariable("resource") String resource, @PathVariable("id") String id, @RequestBody Patch patch) throws PatchException {
		PersistenceCallback<?> persistenceCallback = callbackRegistry.findPersistenceCallback(resource);		
		Object findOne = persistenceCallback.findOne(id);
		return applyAndDiff(patch, findOne, persistenceCallback);
	}

	
	@ExceptionHandler(PatchException.class)
	@ResponseStatus(value=HttpStatus.CONFLICT, reason="Unable to apply patch")
	public void handlePatchException(PatchException e) {}
	
	
	@SuppressWarnings("unchecked")
	private <T> Patch applyAndDiff(Patch patch, Object target, PersistenceCallback<T> persistenceCallback) throws PatchException {
		DiffSync<T> sync = new DiffSync<>(shadowStore, persistenceCallback.getEntityType());
		T patched = sync.apply((T) target, patch);
		persistenceCallback.persistChange(patched);
		return sync.diff(patched);
	}
	
	private <T> Patch applyAndDiffAgainstList(Patch patch, List<T> target, PersistenceCallback<T> persistenceCallback) throws PatchException {
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
