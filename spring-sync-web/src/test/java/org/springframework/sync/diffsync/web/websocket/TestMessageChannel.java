package org.springframework.sync.diffsync.web.websocket;

import lombok.Getter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.AbstractSubscribableChannel;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TestMessageChannel extends AbstractSubscribableChannel {

    private final List<Message<?>> messages = new ArrayList<>();

    @Override
    protected boolean sendInternal(Message<?> message, long timeout) {
        messages.add(message);
        return true;
    }

}
