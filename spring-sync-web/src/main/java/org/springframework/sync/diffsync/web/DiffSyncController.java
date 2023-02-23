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
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.sync.Patch;
import org.springframework.sync.diffsync.exception.PersistenceCallbackNotFoundException;
import org.springframework.sync.diffsync.exception.ResourceNotFoundException;
import org.springframework.sync.diffsync.service.DiffSyncService;
import org.springframework.sync.exception.PatchException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller to handle PATCH requests and apply them to resources using {@link DiffSyncService}.
 *
 * @author Craig Walls
 * @author Michał Kuśmidrowicz
 */
@RestController
@RequestMapping("${spring.diffsync.path:}/rest")
@RequiredArgsConstructor
public class DiffSyncController {

    private static final String UNABLE_TO_APPLY_PATCH_MSG = "Unable to apply patch - %s";

    private final DiffSyncService diffSyncService;

    @PatchMapping("/{resource}")
    @SendTo("/topic/{resource}")
    public Patch patchRest(@PathVariable("resource") String resource, @RequestBody Patch patch) {
        try {
            return diffSyncService.patch(resource, patch);
        } catch (PatchException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(UNABLE_TO_APPLY_PATCH_MSG, e.getMessage()), e);
        } catch (PersistenceCallbackNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(UNABLE_TO_APPLY_PATCH_MSG, e.getMessage()), e);
        }
    }

    @PatchMapping(value = "/{resource}/{id}")
    @SendTo("/topic/{resource}")
    public Patch patchRest(@PathVariable("resource") String resource, @PathVariable("id") String id, @RequestBody Patch patch) {
        try {
            return diffSyncService.patch(resource, id, patch);
        } catch (PatchException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(UNABLE_TO_APPLY_PATCH_MSG, e.getMessage()), e);
        } catch (PersistenceCallbackNotFoundException | ResourceNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(UNABLE_TO_APPLY_PATCH_MSG, e.getMessage()), e);
        }
    }

    @MessageMapping("/{resource}")
    @SendTo("/topic/{resource}")
    public Patch patchWebsocket(@DestinationVariable("resource") String resource, Patch patch) throws PersistenceCallbackNotFoundException, PatchException {
        return diffSyncService.patch(resource, patch);
    }

    @MessageMapping("/{resource}/{id}")
    @SendTo("/topic/{resource}/{id}")
    public Patch patchWebsocket(@DestinationVariable("resource") String resource, @DestinationVariable("id") String id, Patch patch) throws PersistenceCallbackNotFoundException, PatchException, ResourceNotFoundException {
        return diffSyncService.patch(resource, id, patch);
    }

    @MessageExceptionHandler({PatchException.class, PersistenceCallbackNotFoundException.class, ResourceNotFoundException.class})
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return exception.getMessage();
    }

}
