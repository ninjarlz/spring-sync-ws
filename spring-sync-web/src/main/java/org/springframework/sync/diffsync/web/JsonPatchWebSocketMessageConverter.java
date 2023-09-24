package org.springframework.sync.diffsync.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.sync.Patch;
import org.springframework.sync.exception.PatchException;
import org.springframework.sync.json.JsonPatchPatchConverter;

import java.io.IOException;

public class JsonPatchWebSocketMessageConverter extends AbstractMessageConverter {

    private static final MediaType JSON_PATCH = new MediaType("application", "json-patch+json");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonPatchPatchConverter jsonPatchMaker;


    public JsonPatchWebSocketMessageConverter() {
        addSupportedMimeTypes(JSON_PATCH);
        jsonPatchMaker = new JsonPatchPatchConverter();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Patch.class.isAssignableFrom(clazz);
    }

    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
        try {
            if (message.getPayload() instanceof byte[] bytePayload) {
                return jsonPatchMaker.convert(MAPPER.readTree(bytePayload));
            }
            return jsonPatchMaker.convert(MAPPER.valueToTree(message.getPayload()));
        } catch (PatchException | IOException e) {
            return null;
        }
    }

    @Override
    protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
        try {
            JsonNode jsonNode = jsonPatchMaker.convert((Patch) payload);
            return MAPPER.writeValueAsBytes(jsonNode);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
