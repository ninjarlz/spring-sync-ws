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
package org.springframework.sync;

/**
 * Operation that removes the value at the given path.
 * Will throw a {@link PatchException} if the given path isn't valid or if the path is non-nullable.
 * 
 * @author Craig Walls
 */
public class RemoveOperation extends PatchOperation {

	public static final String OP_TYPE = "remove";

	/**
	 * Constructs the remove operation
	 * @param path The path of the value to be removed. (e.g., '/foo/bar/4')
	 */
	public RemoveOperation(String path) {
		super(OP_TYPE, path);
	}
	
	@Override
	<T> void perform(Object target, Class<T> type) throws PatchException {
		popValueAtPath(target, path);
	}

}
