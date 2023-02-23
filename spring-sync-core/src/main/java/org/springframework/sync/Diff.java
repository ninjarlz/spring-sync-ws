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

import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.sync.exception.PatchException;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Provides support for producing a {@link Patch} from the comparison of two objects.
 * @author Craig Walls
 */
public class Diff {

	private static final String DIFF_ERROR_MSG = "Error performing diff:";

	/**
	 * Performs a difference operation between two objects, resulting in a {@link Patch} describing the differences.
	 * 
	 * @param original the original, unmodified object.
	 * @param modified the modified object.
	 * @return a {@link Patch} describing the differences between the two objects.
	 * @throws PatchException if an error occurs while performing the difference.
	 */
	@SuppressWarnings("unchecked")
	public static Patch diff(Object original, Object modified) throws PatchException {
		try {
			List<PatchOperation> operations = new ArrayList<>();
			if (original instanceof List && modified instanceof List) {
				diffList(operations, StringUtils.EMPTY, (List<Object>) original, (List<Object>) modified);
			} else {
				diffNonList(operations, StringUtils.EMPTY, original, modified);
			}
			return new Patch(operations);
		} catch (Exception e) {
			throw new PatchException(DIFF_ERROR_MSG, e);
		}
	}
	
	// private helpers

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void diffList(List<PatchOperation> operations, String path, List<Object> original, List<Object> modified) throws IOException, IllegalAccessException {
		difflib.Patch diff = DiffUtils.diff(original, modified);
		List<Delta> deltas = diff.getDeltas();
		for (Delta delta : deltas) {
			TYPE type = delta.getType();
			int revisedPosition = delta.getRevised().getPosition();

			switch (type) {
				case CHANGE -> {
					List<?> lines = delta.getRevised().getLines();
					for (int offset = 0; offset < lines.size(); offset++) {
						Object originalObject = original.get(revisedPosition + offset);
						Object revisedObject = modified.get(revisedPosition + offset);
						diffNonList(operations, path + "/" + (revisedPosition + offset), originalObject, revisedObject);
					}
				}
				case INSERT -> {
					List<?> lines = delta.getRevised().getLines();
					for (int offset = 0; offset < lines.size(); offset++) {
						operations.add(new AddOperation(path + "/" + (revisedPosition + offset), lines.get(offset)));
					}
				}
				case DELETE -> {
					List<?> lines = delta.getOriginal().getLines();
					for (int offset = 0; offset < lines.size(); offset++) {
						Object originalObject = original.get(revisedPosition + offset);
						operations.add(new TestOperation(path + "/" + revisedPosition, originalObject));
						operations.add(new RemoveOperation(path + "/" + revisedPosition));
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void diffNonList(List<PatchOperation> operations, String path, Object original, Object modified) throws IOException, IllegalAccessException {
		if (!ObjectUtils.nullSafeEquals(original, modified)) {
			if (modified == null) {
				operations.add(new RemoveOperation(path));
				return;
			}
			
			if (isPrimitive(modified)) {
				
				operations.add(new TestOperation(path, original));
				if (original == null) {
					operations.add(new AddOperation(path, modified));
				} else {
					operations.add(new ReplaceOperation(path, modified));
				}
				return;
			}
						
			Class<?> originalType = original.getClass();
			Field[] fields = originalType.getDeclaredFields();
			for (Field field : fields) {
				field.setAccessible(true);
				Class<?> fieldType = field.getType();
				Object origValue = field.get(original);
				Object modValue = field.get(modified);
				if ((fieldType.isArray() || Collection.class.isAssignableFrom(fieldType)) && origValue != null && modValue != null) {
					if (Collection.class.isAssignableFrom(fieldType)) {
						diffList(operations, path + "/" + field.getName(), (List<Object>) origValue, (List<Object>) modValue);
					}
					else if (fieldType.isArray()) {
						diffList(operations, path + "/" + field.getName(), Arrays.asList((Object[]) origValue), Arrays.asList((Object[]) modValue));
					}
				} else {
					diffNonList(operations, path + "/" +field.getName(), origValue, modValue);
				}
			}
		}
	}

	private static boolean isPrimitive(Object o) {
		return o instanceof String || o instanceof Number || o instanceof Boolean;
	}
	
}
