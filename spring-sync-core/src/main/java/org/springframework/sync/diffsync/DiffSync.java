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
package org.springframework.sync.diffsync;

import org.springframework.sync.Diff;
import org.springframework.sync.Patch;
import org.springframework.sync.PatchException;
import org.springframework.sync.util.DeepCloneUtils;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Implements essential steps of the Differential Synchronization routine as described in Neil Fraser's paper at https://neil.fraser.name/writing/sync/eng047-fraser.pdf.
 * </p>
 * 
 * <p>
 * The Differential Synchronization routine can be summarized as follows (with two nodes, A and B):
 * </p>
 * 
 * <ol>
 *   <li>Node A compares a resource with its local shadow of that resource to produce a patch describing the differences</li>
 *   <li>Node A replaces the shadow with the resource.</li>
 *   <li>Node A sends the difference patch to Node B.</li>
 *   <li>Node B applies the patch to its copy of the resource as well as its local shadow of the resource.</li>
 * </ol>
 * 
 * <p>
 * The routine then repeats with Node A and B swapping roles, forming a continuous loop.
 * </p>
 * 
 * <p>
 * To fully understand the Differential Synchronization routine, it's helpful to recognize that a shadow can only be changed by applying a patch or by producing a 
 * difference patch; a resource may be changed by applying a patch or by operations performed outside of the loop.
 * </p>
 * 
 * <p>
 * This class implements the handling of an incoming patch separately from the producing of the outgoing difference patch.
 * It performs no persistence of the patched resources, which is the responsibility of the caller.
 * </p>
 * 
 * @author Craig Walls
 *
 * @param <T> The entity type to perform differential synchronization against.
 */
public class DiffSync<T> {

	private static final String BACKUP_SUFFIX = "_backup";
	
	private final ShadowStore shadowStore;

	private final Class<T> entityType;

	/**
	 * Constructs the Differential Synchronization routine instance.
	 * @param shadowStore the shadow store
	 * @param entityType the type of entity this DiffSync works with
	 */
	public DiffSync(ShadowStore shadowStore, Class<T> entityType) {
		this.shadowStore = shadowStore;
		this.entityType = entityType;
	}
	
	/**
	 * Applies one or more patches to a target object and the target object's shadow, per the Differential Synchronization algorithm.
	 * The target object will remain unchanged and a patched copy will be returned.
	 * 
	 * @param target An object to apply a patch to. Will remain unchanged.
	 * @param patches The patches to be applied.
	 * @return a patched copy of the target.
	 */
	public T apply(T target, Patch...patches) throws PatchException {
		T result = target;
		for (Patch patch : patches) {
			result = apply(patch, result);
		}
		return result;
	}
	
	/**
	 * Applies a patch to a target object and the target object's shadow, per the Differential Synchronization algorithm.
	 * The target object will remain unchanged and a patched copy will be returned.
	 * 
	 * @param patch The patch to be applied.
	 * @param target An object to apply a patch to. Will remain unchanged.
	 * @return a patched copy of the target.
	 */
	public T apply(Patch patch, T target) throws PatchException {
		if (patch.size() == 0) {
			return target;
		}
		Shadow<T> shadow = getShadow(target, false);
		if (patch instanceof VersionedPatch versionedPatch) {
			if (versionedPatch.getServerVersion() < shadow.getServerVersion()) { // e.g., if (patch.serverVersion < shadow.serverVersion)
				shadow = getShadow(target, true);
				putShadow(shadow, false);
			}
		}

		if (shouldApplyPatch(patch, shadow)) {
			shadow = new Shadow<>(patch.apply(shadow.getResource(), entityType), shadow.getServerVersion(), shadow.getClientVersion() + 1);
			Shadow<T> backupShadow = new Shadow<>(shadow.getResource(), shadow.getServerVersion(), shadow.getClientVersion());
			putShadow(shadow, false);
			putShadow(backupShadow, true);
			return patch.apply(DeepCloneUtils.deepClone(target), entityType);
		}
		return target;
	}
	
	/**
	 * Applies one or more patches to a target list and the target list's shadow, per the Differential Synchronization algorithm.
	 * The target object will remain unchanged and a patched copy will be returned.
	 * 
	 * @param patches The patch to be applied.
	 * @param target A list to apply a patch to. Will remain unchanged.
	 * @return a patched copy of the target.
	 */
	public List<T> apply(List<T> target, Patch...patches) throws PatchException {
		List<T> result = target;
		for (Patch patch : patches) {
			result = apply(patch, result);
		}
		return result;
	}

	/**
	 * Applies a patch to a target list and the target list's shadow, per the Differential Synchronization algorithm.
	 * The target object will remain unchanged and a patched copy will be returned.
	 * 
	 * @param patch The patch to be applied.
	 * @param target A list to apply a patch to. Will remain unchanged.
	 * @return a patched copy of the target.
	 */
	public List<T> apply(Patch patch, List<T> target) throws PatchException {
		if (patch.size() == 0) {
			return target;
		}
		Shadow<List<T>> shadow = getShadow(target, false);
		if (patch instanceof VersionedPatch versionedPatch) {
			if (versionedPatch.getServerVersion() < shadow.getServerVersion()) {
				shadow = getShadow(target, true);
				putListShadow(shadow, false);
			}
		}
		
		if (shouldApplyPatch(patch, shadow)) {
			shadow = new Shadow<>(patch.apply(shadow.getResource(), entityType), shadow.getServerVersion(), shadow.getClientVersion() + 1);
			Shadow<List<T>> backupShadow = new Shadow<>(shadow.getResource(), shadow.getServerVersion(), shadow.getClientVersion());
			putListShadow(shadow, false);
			putListShadow(backupShadow, true);
			return patch.apply(DeepCloneUtils.deepClone(target), entityType);
		}
		return target;
	}
	
	/**
	 * Compares a target object with its shadow, producing a patch describing the difference.
	 * Upon completion, the shadow will be replaced with the target, per the Differential Synchronization algorithm.
	 * @param target The target object to produce a difference patch for.
	 * @return a {@link VersionedPatch} describing the differences between the target and its shadow.
	 */
	public VersionedPatch diff(T target) throws PatchException {
		Shadow<T> shadow = getShadow(target, false);
		Patch diff = Diff.diff(shadow.getResource(), target);
		VersionedPatch vDiff = new VersionedPatch(diff.getOperations(), shadow.getServerVersion(), shadow.getClientVersion());
		T patched = diff.apply(shadow.getResource(), entityType);
		shadow = new Shadow<>(patched, shadow.getServerVersion() + 1, shadow.getClientVersion());
		putShadow(shadow, false);
		return vDiff;
	}
	
	/**
	 * Compares a target list with its shadow, producing a patch describing the difference.
	 * Upon completion, the shadow will be replaced with the target, per the Differential Synchronization algorithm.
	 * @param target The target list to produce a difference patch for.
	 * @return a {@link VersionedPatch} describing the differences between the target and its shadow.
	 */
	public VersionedPatch diff(List<T> target) throws PatchException {
		Shadow<List<T>> shadow = getShadow(target, false);
		Patch diff = Diff.diff(shadow.getResource(), target);
		VersionedPatch vDiff = new VersionedPatch(diff.getOperations(), shadow.getServerVersion(), shadow.getClientVersion());
		List<T> patched = diff.apply(shadow.getResource(), entityType);
		shadow = new Shadow<>(patched, shadow.getServerVersion() + 1, shadow.getClientVersion());
		putListShadow(shadow, false);
		return vDiff;
	}
	
	// private helper methods
	
	private boolean shouldApplyPatch(Patch patch, Shadow<?> shadow) {
		if (!(patch instanceof VersionedPatch versionedPatch)) {
			return true;
		}
		return versionedPatch.getServerVersion() == shadow.getServerVersion() && versionedPatch.getClientVersion() == shadow.getClientVersion();
	}
	
	@SuppressWarnings("unchecked")
	private Shadow<T> getShadow(T target, boolean getBackup) {
		String shadowStoreKey = getShadowStoreKey(target);
		if (getBackup) {
			shadowStoreKey += BACKUP_SUFFIX;
		}
		Shadow<T> shadow = (Shadow<T>) shadowStore.getShadow(shadowStoreKey);
		if (Objects.isNull(shadow)) {
			shadow = new Shadow<>(DeepCloneUtils.deepClone(target), 0, 0); // OKAY
		}
		return shadow;
	}

	private void putShadow(Shadow<T> shadow, boolean isBackup) {
		String shadowStoreKey = getShadowStoreKey(shadow.getResource());
		if (isBackup) {
			shadowStoreKey += BACKUP_SUFFIX;
		}
		shadowStore.putShadow(shadowStoreKey, shadow);
	}

	private void putListShadow(Shadow<List<T>> shadow, boolean isBackup) {
		String shadowStoreKey = getShadowStoreKey(shadow.getResource());
		if (isBackup) {
			shadowStoreKey += BACKUP_SUFFIX;
		}
		shadowStore.putShadow(shadowStoreKey, shadow);
	}

	@SuppressWarnings("unchecked")
	private Shadow<List<T>> getShadow(List<T> target, boolean getBackup) {
		String shadowStoreKey = getShadowStoreKey(target);
		if (getBackup) {
			shadowStoreKey += BACKUP_SUFFIX;
		}
		Shadow<List<T>> shadow = (Shadow<List<T>>) shadowStore.getShadow(shadowStoreKey);
		if (Objects.isNull(shadow)) {
			shadow = new Shadow<>(DeepCloneUtils.deepClone(target), 0, 0); // OKAY
		}
		return shadow;
	}

	private String getShadowStoreKey(T t) {
		return "shadow/" + entityType.getSimpleName();
	}
	
	private String getShadowStoreKey(List<T> t) {
		return "shadow/" + entityType.getSimpleName() + "List";
	}

}
