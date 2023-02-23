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
package org.springframework.sync.diffsync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.ShadowStore;
import org.springframework.sync.diffsync.shadowstore.MapBasedShadowStore;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

/**
 * Adapter implementation of {@link DiffSyncConfigurer} providing default implementations.
 * @author Craig Walls
 */
public class DiffSyncConfigurerAdapter implements DiffSyncConfigurer {

	@Value( "${:spring.diffsync.path:}" )
	private String diffSyncPath;

	@Override
	public void addPersistenceCallbacks(PersistenceCallbackRegistry registry) {
	}
	
	@Override
	public ShadowStore getShadowStore(String remoteNodeId) {
		return new MapBasedShadowStore(remoteNodeId);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic", "/queue");
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		String path = String.format("%s/websocket", diffSyncPath);
		registry.addEndpoint(path).setAllowedOrigins("*");
		registry.addEndpoint(path).setAllowedOrigins("*").withSockJS();
	}

}
