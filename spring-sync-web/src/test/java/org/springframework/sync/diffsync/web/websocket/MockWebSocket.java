package org.springframework.sync.diffsync.web.websocket;

import lombok.Getter;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;

@Getter
public class MockWebSocket {

    private static final MediaType JSON_PATCH = new MediaType("application", "json-patch+json");

    private final TestMessageChannel clientOutboundChannel;
    private final TestMessageChannel clientInboundChannel;
    private final TestMessageChannel brokerChannel;
    private final TestAnnotationMethodHandler annotationMethodHandler;

    public MockWebSocket(Object handler) {
        clientOutboundChannel = new TestMessageChannel();
        clientInboundChannel = new TestMessageChannel();
        brokerChannel = new TestMessageChannel();
        annotationMethodHandler =
                new TestAnnotationMethodHandler(clientInboundChannel, clientOutboundChannel, new SimpMessagingTemplate(brokerChannel));
        annotationMethodHandler.registerHandler(handler);
        annotationMethodHandler.setDestinationPrefixes(Collections.singletonList("/app"));
        annotationMethodHandler.setMessageConverter(new MappingJackson2MessageConverter(MediaType.APPLICATION_JSON, JSON_PATCH));
        annotationMethodHandler.setApplicationContext(new StaticApplicationContext());
        annotationMethodHandler.afterPropertiesSet();
    }

    public void handleMessage(Message<?> message) {
        annotationMethodHandler.handleMessage(message);
    }
}
