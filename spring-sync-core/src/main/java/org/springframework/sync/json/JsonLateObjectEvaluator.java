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
package org.springframework.sync.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.sync.LateObjectEvaluator;

/**
 * {@link LateObjectEvaluator} implementation that assumes values represented as JSON objects.
 * @author Craig Walls
 */
class JsonLateObjectEvaluator implements LateObjectEvaluator {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final JsonNode valueNode;

	public JsonLateObjectEvaluator(JsonNode valueNode) {
		this.valueNode = valueNode;
	}
	
	@Override
	public <T> Object evaluate(Class<T> type) {
		try {
			return MAPPER.readValue(valueNode.traverse(), type);
		} catch (Exception e) {
			return null;
		}
	}

}
