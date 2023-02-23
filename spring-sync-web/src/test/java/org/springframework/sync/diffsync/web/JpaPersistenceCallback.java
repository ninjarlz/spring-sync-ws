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

import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.sync.diffsync.PersistenceCallback;
import org.springframework.sync.diffsync.exception.ResourceNotFoundException;

import java.util.List;

@RequiredArgsConstructor
public class JpaPersistenceCallback<T> implements PersistenceCallback<T> {
	
	private final CrudRepository<T, Long> repository;
	private final Class<T> entityType;
	
	@Override
	public List<T> findAll() {
		return (List<T>) repository.findAll();
	}
	
	@Override
	public T findOne(String id) throws ResourceNotFoundException {
		return repository.findById(Long.valueOf(id))
				.orElseThrow(() -> new ResourceNotFoundException(id));
	}
	
	@Override
	public void persistChange(T itemToSave) {
		repository.save(itemToSave);
	}
	
	@Override
	public void persistChanges(List<T> itemsToSave, List<T> itemsToDelete) {
		repository.saveAll(itemsToSave);
		repository.deleteAll(itemsToDelete);
	}

	@Override
	public Class<T> getEntityType() {
		return entityType;
	}
	
}