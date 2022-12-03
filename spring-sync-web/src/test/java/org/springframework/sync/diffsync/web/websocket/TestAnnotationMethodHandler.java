package org.springframework.sync.diffsync.web.websocket;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;

public class TestAnnotationMethodHandler extends SimpAnnotationMethodMessageHandler {

    public TestAnnotationMethodHandler(SubscribableChannel inChannel, MessageChannel outChannel, SimpMessageSendingOperations brokerTemplate) {
        super(inChannel, outChannel, brokerTemplate);
    }

    public void registerHandler(Object handler) {
        super.detectHandlerMethods(handler);
    }
}