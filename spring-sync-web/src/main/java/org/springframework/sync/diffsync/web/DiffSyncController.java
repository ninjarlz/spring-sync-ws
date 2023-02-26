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
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.sync.Patch;
import org.springframework.sync.diffsync.exception.PersistenceCallbackNotFoundException;
import org.springframework.sync.diffsync.exception.ResourceNotFoundException;
import org.springframework.sync.diffsync.service.DiffSyncService;
import org.springframework.sync.exception.PatchException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpSession;
import java.net.URI;

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
    private static final String PATCH_RECEIVED_MSG = "New patch for sessionId '%s' and path '%s' received";
    private static final String UNABLE_TO_APPLY_PATCH_MSG = "Unable to apply patch for sessionId '%s' because of: %s";
    private static final String PATCH_APPLIED_MSG = "Patch for sessionId '%s' and path '%s' applied";
    private final DiffSyncService diffSyncService;
    private final SimpMessageSendingOperations brokerTemplate;

    @PatchMapping(value = "/{resource}", consumes = JSON_PATCH_VALUE, produces = JSON_PATCH_VALUE)
    public ResponseEntity<Patch> patchRest(HttpSession session, @PathVariable("resource") String resource, @RequestBody Patch patch) {
        try {
            log.info(String.format(PATCH_RECEIVED_MSG, session.getId(), "/" + resource));
            patch = diffSyncService.patch(resource, patch);
            log.info(String.format(PATCH_APPLIED_MSG, session.getId(), "/" + resource));
            String destination = String.format("/topic/%s", resource);
            brokerTemplate.convertAndSend(destination, patch);
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(JSON_PATCH)
                    .location(getCurrentURI())
                    .body(patch);
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
            String resourcePath = String.format("/%s/%s", resource, id);
            log.info(String.format(PATCH_RECEIVED_MSG, session.getId(), resourcePath));
            patch = diffSyncService.patch(resource, id, patch);
            log.info(String.format(PATCH_APPLIED_MSG, session.getId(), resourcePath));
            String destination = String.format("/topic%s", resourcePath);
            brokerTemplate.convertAndSend(destination, patch);
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(JSON_PATCH)
                    .location(getCurrentURI())
                    .body(patch);
        } catch (PatchException e) {
            log.error(String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), ExceptionUtils.getStackTrace(e)));
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), e.getMessage()), e);
        } catch (PersistenceCallbackNotFoundException | ResourceNotFoundException e) {
            log.error(String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), ExceptionUtils.getStackTrace(e)));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(UNABLE_TO_APPLY_PATCH_MSG, session.getId(), e.getMessage()), e);
        }
    }

    @MessageMapping("/{resource}")
    @SendTo("/topic/{resource}")
    public Patch patchWebsocket(@DestinationVariable("resource") String resource, Patch patch) throws PersistenceCallbackNotFoundException, PatchException {
        String sessionId = SimpAttributesContextHolder.currentAttributes().getSessionId();
        log.info(String.format(PATCH_RECEIVED_MSG, sessionId, "/" + resource));
        patch = diffSyncService.patch(resource, patch);
        log.info(String.format(PATCH_APPLIED_MSG, sessionId, "/" + resource));
        return patch;
    }

    @MessageMapping("/{resource}/{id}")
    @SendTo("/topic/{resource}/{id}")
    public Patch patchWebsocket(@DestinationVariable("resource") String resource, @DestinationVariable("id") String id, Patch patch) throws PersistenceCallbackNotFoundException, PatchException, ResourceNotFoundException {
        String sessionId = SimpAttributesContextHolder.currentAttributes().getSessionId();
        String resourcePath = String.format("/%s/%s", resource, id);
        log.info(String.format(PATCH_RECEIVED_MSG, sessionId, resourcePath));
        patch = diffSyncService.patch(resource, id, patch);
        log.info(String.format(PATCH_APPLIED_MSG, sessionId, resourcePath));
        return patch;
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
}
