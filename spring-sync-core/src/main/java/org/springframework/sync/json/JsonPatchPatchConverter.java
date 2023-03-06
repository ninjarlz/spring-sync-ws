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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.sync.*;
import org.springframework.sync.exception.PatchException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Convert {@link JsonNode}s containing JSON Patch to/from {@link Patch} objects.
 * @author Craig Walls
 */
public class JsonPatchPatchConverter implements PatchConverter<JsonNode> {

	private static final String UNRECOGNIZED_OPERATION_TYPE_MSG = "Unrecognized operation type: ";
	private static final String INVALID_JSON_NODE_MSG = "JsonNode must be an instance of ArrayNode";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Constructs a {@link Patch} object given a JsonNode.
	 * @param jsonNode a JsonNode containing the JSON Patch
	 * @return a {@link Patch}
	 */
	public Patch convert(JsonNode jsonNode) throws PatchException {
		if (!(jsonNode instanceof ArrayNode opNodes)) {
			throw new PatchException(INVALID_JSON_NODE_MSG);
		}
		List<PatchOperation> ops = new ArrayList<>(opNodes.size());
		for (Iterator<JsonNode> elements = opNodes.elements(); elements.hasNext(); ) {
			JsonNode opNode = elements.next();

			String opType = opNode.get(PatchOperation.OP_ENTRY).textValue();
			String path = opNode.get(PatchOperation.PATH_ENTRY).textValue();

			JsonNode valueNode = opNode.get(PatchOperation.VALUE_ENTRY);
			Object value = valueFromJsonNode(path, valueNode);
			String from = opNode.has(FromOperation.FROM_ENTRY) ? opNode.get(FromOperation.FROM_ENTRY).textValue() : null;
			switch (opType) {
				case TestOperation.OP_TYPE -> ops.add(new TestOperation(path, value));
				case ReplaceOperation.OP_TYPE -> ops.add(new ReplaceOperation(path, value));
				case RemoveOperation.OP_TYPE -> ops.add(new RemoveOperation(path));
				case AddOperation.OP_TYPE -> ops.add(new AddOperation(path, value));
				case CopyOperation.OP_TYPE -> ops.add(new CopyOperation(path, from));
				case MoveOperation.OP_TYPE -> ops.add(new MoveOperation(path, from));
				default -> throw new PatchException(UNRECOGNIZED_OPERATION_TYPE_MSG + opType);
			}
		}
		return new Patch(ops);
	}

	/**
	 * Renders a {@link Patch} as a {@link JsonNode}.
	 * @param patch the patch
	 * @return a {@link JsonNode} containing JSON Patch.
	 */
	public JsonNode convert(Patch patch) {
		List<PatchOperation> operations = patch.getOperations();
		JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
		ArrayNode patchNode = nodeFactory.arrayNode();
		for (PatchOperation operation : operations) {
			ObjectNode opNode = nodeFactory.objectNode();
			opNode.set(PatchOperation.OP_ENTRY, nodeFactory.textNode(operation.getOp()));
			opNode.set(PatchOperation.PATH_ENTRY, nodeFactory.textNode(operation.getPath()));
			if (operation instanceof FromOperation fromOp) {
				opNode.set(FromOperation.FROM_ENTRY, nodeFactory.textNode(fromOp.getFrom()));
			}
			Object value = operation.getValue();
			if (Objects.nonNull(value)) {
				if (value instanceof JsonLateObjectEvaluator jsonLateObjectEvaluator) {
					opNode.set(PatchOperation.VALUE_ENTRY, jsonLateObjectEvaluator.getValueNode());
				} else {
					opNode.set(PatchOperation.VALUE_ENTRY, MAPPER.valueToTree(value));
				}
			}
			patchNode.add(opNode);
		}
		return patchNode;
	}

	private Object valueFromJsonNode(String path, JsonNode valueNode) {
		if (Objects.isNull(valueNode) || valueNode.isNull()) {
			return null;
		}
		if (valueNode.isTextual()) {
			return valueNode.asText();
		}
		if (valueNode.isFloatingPointNumber()) {
			return valueNode.asDouble();
		}
		if (valueNode.isBoolean()) {
			return valueNode.asBoolean();
		}
		if (valueNode.isInt()) {
			return valueNode.asInt();
		}
		if (valueNode.isLong()) {
			return valueNode.asLong();
		}
		if (valueNode.isObject()) {
			return new JsonLateObjectEvaluator(valueNode);
		}
		if (valueNode.isArray()) {
			// TODO: Convert valueNode to array
		}

		return null;
	}




}
