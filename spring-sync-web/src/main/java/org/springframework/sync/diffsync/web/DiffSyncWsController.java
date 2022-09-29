package org.springframework.sync.diffsync.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.sync.diffsync.Equivalency;
import org.springframework.sync.diffsync.IdPropertyEquivalency;
import org.springframework.sync.diffsync.PersistenceCallbackRegistry;
import org.springframework.sync.diffsync.ShadowStore;
import org.springframework.sync.diffsync.service.DiffSyncService;

@Controller
public class DiffSyncWsController {

    private static final String UNABLE_TO_APPLY_PATCH_MSG = "Unable to apply patch - %s";

    private final PersistenceCallbackRegistry callbackRegistry;
    private final DiffSyncService diffSyncService;

    @Autowired
    public DiffSyncWsController(PersistenceCallbackRegistry callbackRegistry, DiffSyncService diffSyncService) {
        this.callbackRegistry = callbackRegistry;
        this.diffSyncService = diffSyncService;
    }

//    @MessageMapping("/chat")
//    @SendTo("/topic/messages")
//    public OutputMessage send(Message message) throws Exception {
//        String time = new SimpleDateFormat("HH:mm").format(new Date());
//        return new OutputMessage(message.getFrom(), message.getText(), time);
//    }
}
