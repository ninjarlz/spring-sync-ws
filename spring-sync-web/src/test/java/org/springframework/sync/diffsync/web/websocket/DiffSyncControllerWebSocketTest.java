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
package org.springframework.sync.diffsync.web.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.sync.Patch;
import org.springframework.sync.PatchOperation;
import org.springframework.sync.Todo;
import org.springframework.sync.TodoRepository;
import org.springframework.sync.diffsync.EmbeddedDataSourceConfig;
import org.springframework.sync.diffsync.Equivalency;
import org.springframework.sync.diffsync.IdPropertyEquivalency;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.service.DiffSyncService;
import org.springframework.sync.diffsync.service.impl.DiffSyncServiceImpl;
import org.springframework.sync.diffsync.shadowstore.MapBasedShadowStore;
import org.springframework.sync.diffsync.shadowstore.WebSocketShadowStore;
import org.springframework.sync.diffsync.web.DiffSyncController;
import org.springframework.sync.diffsync.web.JpaPersistenceCallback;
import org.springframework.sync.diffsync.web.JsonPatchHttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EmbeddedDataSourceConfig.class})
@Transactional
@Sql(value = "/org/springframework/sync/db-scripts/reset-id-sequence.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DiffSyncControllerWebSocketTest {

    private static final String RESOURCE_PATH = "/todos";
    private static final String APP_WEBSOCKET_RESOURCE_PATH = "/app" + RESOURCE_PATH;
    private static final String TOPIC_WEBSOCKET_RESOURCE_PATH = "/topic" + RESOURCE_PATH;
    private static final String WEBSOCKET_SESSION_ID = "0";

    @Autowired
    private TodoRepository repository;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON_PATCH = new MediaType("application", "json-patch+json");

    //
    // entity patching WebSocket
    //

    @Test
    public void patchSendsEntityStatusChange() throws Exception {
        TodoRepository todoRepository = todoRepository();
        TestMessageChannel brokerChannel = new TestMessageChannel();
        SimpMessageSendingOperations brokerTemplate = new SimpMessagingTemplate(brokerChannel);
        DiffSyncController diffSyncController = diffSyncController(todoRepository, brokerTemplate);
        MockWebSocket mockWebSocket = mockWebSocket(diffSyncController, brokerTemplate);

        StompHeaderAccessor sendHeaders = buildStompHeaderAccessor(APP_WEBSOCKET_RESOURCE_PATH + "/2");
        Message<Patch> sendMessage = MessageBuilder
                .withPayload(patchResource("patch-change-entity-status"))
                .setHeaders(sendHeaders)
                .build();
        mockWebSocket.handleMessage(sendMessage);

        assertEquals(1, brokerChannel.getMessages().size());
        Message<?> reply = brokerChannel.getMessages().get(0);
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        assertEquals(TOPIC_WEBSOCKET_RESOURCE_PATH + "/2", replyHeaders.getDestination());
        assertEquals(WEBSOCKET_SESSION_ID, replyHeaders.getSessionId());
        Patch payload = (Patch) reply.getPayload();
        assertEquals(payload.size(), 0);

        List<Todo> all = (List<Todo>) repository.findAll();
        assertEquals(3, all.size());
        assertEquals(new Todo(1L, "A", false), all.get(0));
        assertEquals(new Todo(2L, "B", true), all.get(1));
        assertEquals(new Todo(3L, "C", false), all.get(2));
    }

    @Test
    public void patchSendsEntityDescriptionChange() throws Exception {
        TodoRepository todoRepository = todoRepository();
        TestMessageChannel brokerChannel = new TestMessageChannel();
        SimpMessageSendingOperations brokerTemplate = new SimpMessagingTemplate(brokerChannel);
        DiffSyncController diffSyncController = diffSyncController(todoRepository, brokerTemplate);
        MockWebSocket mockWebSocket = mockWebSocket(diffSyncController, brokerTemplate);

        StompHeaderAccessor sendHeaders = buildStompHeaderAccessor(APP_WEBSOCKET_RESOURCE_PATH + "/2");
        Message<Patch> sendMessage = MessageBuilder
                .withPayload(patchResource("patch-change-entity-description"))
                .setHeaders(sendHeaders)
                .build();
        mockWebSocket.handleMessage(sendMessage);

        assertEquals(1, brokerChannel.getMessages().size());
        Message<?> reply = brokerChannel.getMessages().get(0);
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        assertEquals(TOPIC_WEBSOCKET_RESOURCE_PATH + "/2", replyHeaders.getDestination());
        assertEquals(WEBSOCKET_SESSION_ID, replyHeaders.getSessionId());
        Patch payload = (Patch) reply.getPayload();
        assertEquals(payload.size(), 0);

        List<Todo> all = (List<Todo>) repository.findAll();
        assertEquals(3, all.size());
        assertEquals(new Todo(1L, "A", false), all.get(0));
        assertEquals(new Todo(2L, "BBB", false), all.get(1));
        assertEquals(new Todo(3L, "C", false), all.get(2));
    }

    //
    // private helpers
    //

    private Patch patchResource(String name) throws IOException {
        ClassPathResource resource = new ClassPathResource("/org/springframework/sync/json-payloads/" + name + ".json");
        List<PatchOperation> patchOperations = MAPPER.readValue(resource.getInputStream(), new TypeReference<>() {});
        return new Patch(patchOperations);
    }

    private String resource(String name) throws IOException {
        ClassPathResource resource = new ClassPathResource("/org/springframework/sync/json-payloads/" + name + ".json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
        StringBuilder builder = new StringBuilder();
        while (reader.ready()) {
            builder.append(reader.readLine());
        }
        return builder.toString();
    }

    private TodoRepository todoRepository() {
        return repository;
    }

    private DiffSyncController diffSyncController(TodoRepository todoRepository,SimpMessageSendingOperations brokerTemplate) {
        PersistenceCallbackRegistry callbackRegistry = new PersistenceCallbackRegistry();
        callbackRegistry.addPersistenceCallback(new JpaPersistenceCallback<>(todoRepository, Todo.class));
        WebSocketShadowStore webSocketShadowStore = new MapBasedShadowStore(WEBSOCKET_SESSION_ID);
        Equivalency equivalency = new IdPropertyEquivalency();
        DiffSyncService diffSyncService = new DiffSyncServiceImpl(callbackRegistry, equivalency);
        return new DiffSyncController(null, webSocketShadowStore, diffSyncService, brokerTemplate);
    }

    private MockWebSocket mockWebSocket(DiffSyncController diffSyncController, SimpMessageSendingOperations brokerTemplate) {
        return new MockWebSocket(diffSyncController, brokerTemplate);
    }

    private MockMvc mockMvc(DiffSyncController diffSyncController) {
        return standaloneSetup(diffSyncController)
                .setMessageConverters(new JsonPatchHttpMessageConverter(), new MappingJackson2HttpMessageConverter())
                .build();
    }

    private StompHeaderAccessor buildStompHeaderAccessor(String destination) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(destination);
        headers.setSessionId(WEBSOCKET_SESSION_ID);
        headers.setSessionAttributes(Collections.emptyMap());
        headers.setContentType(JSON_PATCH);
        return headers;
    }
}
