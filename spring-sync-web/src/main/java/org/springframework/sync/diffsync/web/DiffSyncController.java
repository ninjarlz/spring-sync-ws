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
package org.springframework.sync.diffsync.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.sync.Patch;
import org.springframework.sync.diffsync.exception.PersistenceCallbackNotFoundException;
import org.springframework.sync.diffsync.exception.ResourceNotFoundException;
import org.springframework.sync.diffsync.service.DiffSyncService;
import org.springframework.sync.diffsync.shadowstore.ShadowStore;
import org.springframework.sync.exception.PatchException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpSession;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * Controller to handle PATCH requests and apply them to resources using {@link DiffSyncService}.
 *
 * @author Craig Walls
 * @author Michał Kuśmidrowicz
 */
@RestController
@RequestMapping("${spring.diff-sync.path:}/rest")
@RequiredArgsConstructor
@Log4j2
public class DiffSyncController {
    private static final MediaType JSON_PATCH = new MediaType("application", "json-patch+json");
    private static final String JSON_PATCH_VALUE = "application/json-patch+json";
    private static final String TOPIC_DESTINATION = "/topic";
    private static final String PATCH_RECEIVED_MSG = "New patch for sessionId '%s' and path '%s' received";
    private static final String UNABLE_TO_APPLY_PATCH_MSG = "Unable to apply patch for sessionId '%s' because of: %s";
    private static final String PATCH_APPLIED_MSG = "Patch for sessionId '%s' and path '%s' applied";

    private final ShadowStore restShadowStore;
    private final ShadowStore webSocketShadowStore;
    private final DiffSyncService diffSyncService;
    private final SimpMessageSendingOperations brokerTemplate;

    @PatchMapping(value = "/{resource}", consumes = JSON_PATCH_VALUE, produces = JSON_PATCH_VALUE)
    public ResponseEntity<Patch> patchRest(HttpSession session, @PathVariable("resource") String resource, @RequestBody Patch patch) {
        try {
            log.info(String.format(PATCH_RECEIVED_MSG, session.getId(), "/" + resource));
            String resourceDestination = String.format("%s/%s", TOPIC_DESTINATION, resource);
            Patch modifiedPatch = diffSyncService.patch(restShadowStore, resource, patch);
            log.info(String.format(PATCH_APPLIED_MSG, session.getId(), "/" + resource));
            websocketBroadcastPatch(resourceDestination, patch, modifiedPatch);
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(JSON_PATCH)
                    .location(getCurrentURI())
                    .body(modifiedPatch);
        } catch (PatchException e) {
            log.error(String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), ExceptionUtils.getStackTrace(e)));
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), e.getMessage()), e);
        } catch (PersistenceCallbackNotFoundException e) {
            log.error(String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), ExceptionUtils.getStackTrace(e)));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), e.getMessage()), e);
        }
    }

    @PatchMapping(value = "/{resource}/{id}", consumes = JSON_PATCH_VALUE, produces = JSON_PATCH_VALUE)
    public ResponseEntity<Patch> patchRest(HttpSession session, @PathVariable("resource") String resource, @PathVariable("id") String id, @RequestBody Patch patch) {
        try {
            String objectPath = String.format("/%s/%s", resource, id);
            String resourceDestination = String.format("%s/%s", TOPIC_DESTINATION, resource);
            String objectDestination = String.format("%s%s", TOPIC_DESTINATION, objectPath);
            log.info(String.format(PATCH_RECEIVED_MSG, session.getId(), objectPath));
            Patch modifiedPatch = diffSyncService.patch(restShadowStore, resource, id, patch);
            log.info(String.format(PATCH_APPLIED_MSG, session.getId(), objectPath));
            websocketBroadcastPatch(objectDestination, patch, modifiedPatch);
            websocketBroadcastPatch(resourceDestination, patch, modifiedPatch);
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(JSON_PATCH)
                    .location(getCurrentURI())
                    .body(modifiedPatch);
        } catch (PatchException e) {
            log.error(String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), ExceptionUtils.getStackTrace(e)));
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), e.getMessage()), e);
        } catch (PersistenceCallbackNotFoundException | ResourceNotFoundException e) {
            log.error(String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), ExceptionUtils.getStackTrace(e)));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), e.getMessage()), e);
        }
    }

    @MessageMapping("/{resource}")
    public void patchWebsocket(@DestinationVariable("resource") String resource, Patch patch) throws PersistenceCallbackNotFoundException, PatchException {
        String sessionId = SimpAttributesContextHolder.currentAttributes().getSessionId();
        String resourceDestination = String.format("%s/%s", TOPIC_DESTINATION, resource);
        log.info(String.format(PATCH_RECEIVED_MSG, sessionId, "/" + resource));
        Patch modifiedPatch = diffSyncService.patch(webSocketShadowStore, resource, patch);
        log.info(String.format(PATCH_APPLIED_MSG, sessionId, "/" + resource));
        websocketBroadcastPatch(resourceDestination, patch, modifiedPatch);
    }

    @MessageMapping("/{resource}/{id}")
    public void patchWebsocket(@DestinationVariable("resource") String resource, @DestinationVariable("id") String id, Patch patch) throws PersistenceCallbackNotFoundException, PatchException, ResourceNotFoundException {
        String sessionId = SimpAttributesContextHolder.currentAttributes().getSessionId();
        String objectPath = String.format("/%s/%s", resource, id);
        String resourceDestination = String.format("%s/%s", TOPIC_DESTINATION, resource);
        String objectDestination = String.format("%s%s", TOPIC_DESTINATION, objectPath);
        log.info(String.format(PATCH_RECEIVED_MSG, sessionId, objectPath));
        Patch modifiedPatch = diffSyncService.patch(webSocketShadowStore, resource, id, patch);
        log.info(String.format(PATCH_APPLIED_MSG, sessionId, objectPath));
        websocketBroadcastPatch(objectDestination, patch, modifiedPatch);
        websocketBroadcastPatch(resourceDestination, patch, modifiedPatch);
    }

    @MessageExceptionHandler({PatchException.class, PersistenceCallbackNotFoundException.class, ResourceNotFoundException.class})
    @SendToUser("/queue/errors")
    public String handleException(Throwable e) {
        String sessionId = SimpAttributesContextHolder.currentAttributes().getSessionId();
        log.error(String.format(UNABLE_TO_APPLY_PATCH_MSG, sessionId, ExceptionUtils.getStackTrace(e)));
        return String.format(UNABLE_TO_APPLY_PATCH_MSG, sessionId, e.getMessage());
    }

    private URI getCurrentURI() {
        return ServletUriComponentsBuilder.fromCurrentRequestUri()
                .build()
                .toUri();
    }

    private void websocketBroadcastPatch(String destination, Patch patch, Patch modifiedPatch) {
        Map<String, Object> stompHeaders = buildStompHeaders(destination);
        brokerTemplate.convertAndSend(destination, patch, stompHeaders);
        brokerTemplate.convertAndSend(destination, modifiedPatch, stompHeaders);
    }

    private Map<String, Object> buildStompHeaders(String destination) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(destination);
        headers.setSessionAttributes(Collections.emptyMap());
        headers.setContentType(JSON_PATCH);
        return headers.toMap();
    }
}
