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
 * Operation that replaces the value at the given path with a new value.
 * 
 * @author Craig Walls
 */
public class ReplaceOperation extends PatchOperation {

	public static final String OP_TYPE = "replace";

	/**
	 * Constructs the replace operation
	 * @param path The path whose value is to be replaced. (e.g., '/foo/bar/4')
	 * @param value The value that will replace the current path value.
	 */
	public ReplaceOperation(String path, Object value) {
		super(OP_TYPE, path, value);
	}
	
	@Override
	<T> void perform(Object target, Class<T> type) {
		setValueOnTarget(target, evaluateValueFromTarget(target, type));
	}
	
}
