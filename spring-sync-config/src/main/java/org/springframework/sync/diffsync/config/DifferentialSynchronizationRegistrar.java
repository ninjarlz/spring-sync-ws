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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.sync.diffsync.Equivalency;
import org.springframework.sync.diffsync.IdPropertyEquivalency;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.service.DiffSyncService;
import org.springframework.sync.diffsync.service.impl.DiffSyncServiceImpl;
import org.springframework.sync.diffsync.shadowstore.MapBasedShadowStore;
import org.springframework.sync.diffsync.shadowstore.ShadowStore;
import org.springframework.sync.diffsync.web.DiffSyncController;
import org.springframework.util.Assert;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Objects;

/**
 * Configuration adapter for Differential Synchronization in Spring.
 *
 * @author Craig Walls
 * @author Michał Kuśmidrowicz
 */
@Configuration
public class DifferentialSynchronizationRegistrar {

    private static final String DIFF_SYNC_CONFIGURERS_MSG = "At least one configuration class must implement DiffSyncConfigurer";

    private List<DiffSyncConfigurer> diffSyncConfigurers;

    @Autowired
    public void setDiffSyncConfigurers(List<DiffSyncConfigurer> diffSyncConfigurers) {
        Assert.notNull(diffSyncConfigurers, DIFF_SYNC_CONFIGURERS_MSG);
        Assert.notEmpty(diffSyncConfigurers, DIFF_SYNC_CONFIGURERS_MSG);
        this.diffSyncConfigurers = diffSyncConfigurers;
    }

    @Bean
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public ShadowStore restShadowStore(HttpSession session) {
        return buildShadowStore(session.getId());
    }

    @Bean
    @Scope(value = "websocket", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public ShadowStore webSocketShadowStore() {
        String sessionId = SimpAttributesContextHolder.currentAttributes().getSessionId();
        return buildShadowStore(sessionId);
    }

    @Bean
    public PersistenceCallbackRegistry persistenceCallbackRegistry() {
        PersistenceCallbackRegistry registry = new PersistenceCallbackRegistry();
        diffSyncConfigurers.forEach(diffSyncConfigurer -> diffSyncConfigurer.addPersistenceCallbacks(registry));
        return registry;
    }

    @Bean
    public Equivalency equivalency() {
        return new IdPropertyEquivalency();
    }

    @Bean
    public DiffSyncService diffSyncService(PersistenceCallbackRegistry callbackRegistry, Equivalency equivalency) {
        return new DiffSyncServiceImpl(callbackRegistry, equivalency);
    }

    @Bean
    public DiffSyncController diffSyncController(DiffSyncService diffSyncService,
                                                 ShadowStore restShadowStore,
                                                 ShadowStore webSocketShadowStore,
                                                 SimpMessageSendingOperations brokerTemplate) {
        return new DiffSyncController(restShadowStore, webSocketShadowStore, diffSyncService, brokerTemplate);
    }

    private ShadowStore buildShadowStore(String sessionId) {
        for (DiffSyncConfigurer diffSyncConfigurer : diffSyncConfigurers) {
            ShadowStore shadowStore = diffSyncConfigurer.getShadowStore(sessionId);
            if (Objects.nonNull(shadowStore)) {
                return shadowStore;
            }
        }
        return new MapBasedShadowStore(sessionId);
    }
}
