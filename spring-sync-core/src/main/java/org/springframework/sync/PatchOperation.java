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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Getter;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.spel.SpelEvaluationException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.sync.PathToSpEL.pathToExpression;
import static org.springframework.sync.PathToSpEL.pathToParentExpression;

/**
 * Abstract base class representing and providing support methods for patch operations.
 * 
 * @author Craig Walls
 */
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		property = PatchOperation.OP_ENTRY,
		visible = true)
@JsonSubTypes({
		@JsonSubTypes.Type(value = AddOperation.class, name = AddOperation.OP_TYPE),
		@JsonSubTypes.Type(value = CopyOperation.class, name = CopyOperation.OP_TYPE),
		@JsonSubTypes.Type(value = MoveOperation.class, name = MoveOperation.OP_TYPE),
		@JsonSubTypes.Type(value = RemoveOperation.class, name = RemoveOperation.OP_TYPE),
		@JsonSubTypes.Type(value = ReplaceOperation.class, name = ReplaceOperation.OP_TYPE),
		@JsonSubTypes.Type(value = TestOperation.class, name = TestOperation.OP_TYPE),
})
public abstract class PatchOperation {

	public static final String PATH_ENTRY = "path";
	public static final String OP_ENTRY = "op";
	public static final String VALUE_ENTRY = "value";

	private static final String PATH_NOT_NULLABLE_MSG = "Path '%s' is not nullable.";
	private static final String UNABLE_TO_GET_VALUE_MSG = "Unable to get value from target";

	@Getter
	protected final String op;

	@Getter
	protected final String path;

	@Getter
	protected final Object value;
	
	protected final Expression spelExpression;

	/**
	 * Constructs the operation.
	 * @param op the operation name. (e.g., 'move')
	 * @param path the path to perform the operation on. (e.g., '/1/description')
	 */
	@JsonCreator
	public PatchOperation(@JsonProperty("op") String op, @JsonProperty("path") String path) {
		this(op, path, null);
	}
	
	/**
	 * Constructs the operation.
	 * @param op the operation name. (e.g., 'move')
	 * @param path the path to perform the operation on. (e.g., '/1/description')
	 * @param value the value to apply in the operation. Could be an actual value or an implementation of {@link LateObjectEvaluator}.
	 */
	@JsonCreator
	public PatchOperation(@JsonProperty("op") String op, @JsonProperty("path") String path, @JsonProperty("value") Object value) {
		this.op = op;
		this.path = path;
		this.value = value;
		this.spelExpression = pathToExpression(path);
	}

	/**
	 * Pops a value from the given path.
	 * @param target the target from which to pop a value.
	 * @param removePath the path from which to pop a value. Must be a list.
	 * @return the value popped from the list
	 */
	protected Object popValueAtPath(Object target, String removePath) throws PatchException {
		Integer listIndex = targetListIndex(removePath);
		Expression expression = pathToExpression(removePath);
		Object value = expression.getValue(target);
		if (Objects.isNull(listIndex)) {
			try {
				expression.setValue(target, null);
				return value;
			} catch (NullPointerException | SpelEvaluationException e) {
				throw new PatchException(String.format(PATH_NOT_NULLABLE_MSG, removePath));
			}
		} else {
			Expression parentExpression = pathToParentExpression(removePath);
			List<?> list = (List<?>) parentExpression.getValue(target);
			list.remove(listIndex >= 0 ? listIndex : list.size() - 1);
			return value;
		}
	}
	
	/**
	 * Adds a value to the operation's path.
	 * If the path references a list index, the value is added to the list at the given index.
	 * If the path references an object property, the property is set to the value.
	 * @param target The target object.
	 * @param value The value to add.
	 */
	protected void addValue(Object target, Object value) {
		Expression parentExpression = pathToParentExpression(path);
		Object parent = Optional.ofNullable(parentExpression.getValue(target)).orElse(null);
		Integer listIndex = targetListIndex(path);
		if (!(parent instanceof List) || Objects.isNull(listIndex)) {
			spelExpression.setValue(target, value);
		} else {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) parentExpression.getValue(target);
			int addAtIndex = listIndex >= 0 ? listIndex : list.size();
			list.add(addAtIndex, value);
		}
	}

	/**
	 * Sets a value to the operation's path.
	 * @param target The target object.
	 * @param value The value to set.
	 */
	protected void setValueOnTarget(Object target, Object value) {
		spelExpression.setValue(target, value);
	}

	/**
	 * Retrieves a value from the operation's path.
	 * @param target the target object.
	 * @return the value at the path on the given target object.
	 */
	protected Object getValueFromTarget(Object target) throws PatchException {
		try {
			return spelExpression.getValue(target);
		} catch (ExpressionException e) {
			throw new PatchException(UNABLE_TO_GET_VALUE_MSG, e);
		}
	}
	
	/**
	 * Performs late-value evaluation on the operation value if the value is a {@link LateObjectEvaluator}.
	 * @param targetObject the target object, used as assistance in determining the evaluated object's type.
	 * @param entityType the entityType
	 * @param <T> the entity type
	 * @return the result of late-value evaluation if the value is a {@link LateObjectEvaluator}; the value itself otherwise.
	 */
	protected <T> Object evaluateValueFromTarget(Object targetObject, Class<T> entityType) {
		return value instanceof LateObjectEvaluator ? ((LateObjectEvaluator) value).evaluate(entityType) : value;		
	}

	/**
	 * Perform the operation.
	 * @param target the target of the operation.
	 */
	abstract <T> void perform(Object target, Class<T> type) throws PatchException;

	// private helpers
	
	private Integer targetListIndex(String path) {
		String[] pathNodes = path.split("\\/");
		
		String lastNode = pathNodes[pathNodes.length - 1];
		
		if ("~".equals(lastNode)) {
			return -1;
		}
		
		try {
			return Integer.parseInt(lastNode);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	

}
