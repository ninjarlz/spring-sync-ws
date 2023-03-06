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
import org.springframework.sync.exception.PatchException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * <p>Operation to test values on a given target.</p>
 * 
 * <p>
 * If the value given matches the value given at the path, the operation completes as a no-op.
 * On the other hand, if the values do not match or if there are any errors interpreting the path,
 * a {@link PatchException} will be thrown.
 * </p>
 * 
 * @author Craig Walls
 */
public class TestOperation extends PatchOperation {

	public static final String OP_TYPE = "test";

	private static final String TEST_AGAINST_PATH_FAIL_MSG = "Test against path '%s' failed.";

	/**
	 * Constructs the test operation
	 * @param path The path to test. (e.g., '/foo/bar/4')
	 * @param value The value to test the path against.
	 */
 	@JsonCreator
	public TestOperation(@JsonProperty("path") String path, @JsonProperty("value") Object value) {
		super(OP_TYPE, path, value);
	}
	
	@Override
	<T> void perform(Object target, Class<T> type) throws PatchException {
		Object expected = normalizeIfNumber(evaluateValueFromTarget(target, type));
		Object actual = normalizeIfNumber(getValueFromTarget(target));		
		if (!Objects.equals(expected, actual)) {
			throw new PatchException(String.format(TEST_AGAINST_PATH_FAIL_MSG, path));
		}
	}
	
	private Object normalizeIfNumber(Object expected) {
		if (isFloatingPointNumber(expected)) {
			return BigDecimal.valueOf(((Number) expected).doubleValue());
		}
		if (expected instanceof Number) {
			return BigInteger.valueOf(((Number) expected).longValue());
		}
		return expected;
	}

	private boolean isFloatingPointNumber(Object expected) {
		return expected instanceof Double || expected instanceof Float;
	}
}
