package org.springframework.sync.diffsync.web.websocket;

import lombok.Getter;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.sync.diffsync.web.JsonPatchWebSocketMessageConverter;

import java.util.Collections;

@Getter
public class MockWebSocket {

    private static final MediaType JSON_PATCH = new MediaType("application", "json-patch+json");
    private final SimpMessageSendingOperations brokerTemplate;
    private final TestAnnotationMethodHandler annotationMethodHandler;

    public MockWebSocket(Object handler, SimpMessageSendingOperations brokerTemplate) {
        MessageChannel clientOutboundChannel = new TestMessageChannel();
        SubscribableChannel clientInboundChannel = new TestMessageChannel();
        this.brokerTemplate = brokerTemplate;
        annotationMethodHandler =
                new TestAnnotationMethodHandler(clientInboundChannel, clientOutboundChannel, brokerTemplate);
        annotationMethodHandler.registerHandler(handler);
        annotationMethodHandler.setDestinationPrefixes(Collections.singletonList("/app"));
        annotationMethodHandler.setMessageConverter(new JsonPatchWebSocketMessageConverter());
        annotationMethodHandler.setApplicationContext(new StaticApplicationContext());
        annotationMethodHandler.afterPropertiesSet();
    }

    public void handleMessage(Message<?> message) {
        annotationMethodHandler.handleMessage(message);
    }
}
