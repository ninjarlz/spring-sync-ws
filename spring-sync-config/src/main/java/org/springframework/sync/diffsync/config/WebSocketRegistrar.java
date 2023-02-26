package org.springframework.sync.diffsync.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.Assert;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * Configuration adapter for Differential Synchronization WebSocket communication in Spring.
 * @author Michał Kuśmidrowicz
 */
@Configuration
public class WebSocketRegistrar implements WebSocketMessageBrokerConfigurer {

    private static final MediaType JSON_PATCH = new MediaType("application", "json-patch+json");
    private static final String DIFF_SYNC_CONFIGURERS_MSG = "At least one configuration class must implement DiffSyncConfigurer";

    private List<DiffSyncConfigurer> diffSyncConfigurers;

    @Autowired
    public void setDiffSyncConfigurers(List<DiffSyncConfigurer> diffSyncConfigurers) {
        Assert.notNull(diffSyncConfigurers, DIFF_SYNC_CONFIGURERS_MSG);
        Assert.notEmpty(diffSyncConfigurers, DIFF_SYNC_CONFIGURERS_MSG);
        this.diffSyncConfigurers = diffSyncConfigurers;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        diffSyncConfigurers.forEach(diffSyncConfigurer -> diffSyncConfigurer.configureMessageBroker(config));
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        diffSyncConfigurers.forEach(diffSyncConfigurer -> diffSyncConfigurer.registerStompEndpoints(registry));
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new MappingJackson2MessageConverter(JSON_PATCH, MediaType.APPLICATION_JSON));
        return true;
    }
}
