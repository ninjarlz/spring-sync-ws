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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.sync.Patch;
import org.springframework.sync.PatchException;
import org.springframework.sync.diffsync.DiffSync;
import org.springframework.sync.diffsync.PersistenceCallback;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.service.DiffSyncService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller to handle PATCH requests and apply them to resources using {@link DiffSync}.
 *
 * @author Craig Walls
 */
@RestController
public class DiffSyncController {

    private static final String UNABLE_TO_APPLY_PATCH_MSG = "Unable to apply patch - %s";

    private final PersistenceCallbackRegistry callbackRegistry;
    private final DiffSyncService diffSyncService;

    @Autowired
    public DiffSyncController(PersistenceCallbackRegistry callbackRegistry, DiffSyncService diffSyncService) {
        this.callbackRegistry = callbackRegistry;
        this.diffSyncService = diffSyncService;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @RequestMapping(
            value = "${spring.diffsync.path:}/{resource}",
            method = RequestMethod.PATCH)
    public Patch patch(@PathVariable("resource") String resource, @RequestBody Patch patch) {
        try {
            PersistenceCallback<?> persistenceCallback = callbackRegistry.findPersistenceCallback(resource);
            return diffSyncService.applyAndDiffAgainstList(patch, (List) persistenceCallback.findAll(), persistenceCallback);
        } catch (PatchException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(UNABLE_TO_APPLY_PATCH_MSG, e.getMessage()), e);
        }
    }

    @RequestMapping(
            value = "${spring.diffsync.path:}/{resource}/{id}",
            method = RequestMethod.PATCH)
    public Patch patch(@PathVariable("resource") String resource, @PathVariable("id") String id, @RequestBody Patch patch) {
        try {
            PersistenceCallback<?> persistenceCallback = callbackRegistry.findPersistenceCallback(resource);
            Object findOne = persistenceCallback.findOne(id);
            return diffSyncService.applyAndDiff(patch, findOne, persistenceCallback);
        } catch (PatchException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(UNABLE_TO_APPLY_PATCH_MSG, e.getMessage()), e);
        }
    }

    @MessageMapping("/{resource}")
    @SendTo("/topic/{resource}")
    public Patch patchWs(@DestinationVariable("resource") String resource, Patch patch) {
        return patch(resource, patch);
    }

    @MessageMapping("/{resource}/{id}")
    @SendTo("/topic/{resource}/{id}")
    public Patch patchWs(@DestinationVariable("resource") String resource, @DestinationVariable("id") String id, Patch patch) {
        return patch(resource, id, patch);
    }
}
