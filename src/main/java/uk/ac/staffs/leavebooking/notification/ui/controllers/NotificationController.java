package uk.ac.staffs.leavebooking.notification.ui.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.staffs.leavebooking.notification.ContextFacade;
import uk.ac.staffs.leavebooking.notification.application.dto.NotificationDTO;

import java.util.List;

@RestController
@RequestMapping("/api/users/{recipientId}/notifications")
public class NotificationController {
    private final ContextFacade contextFacade;

    public NotificationController(ContextFacade contextFacade) {
        this.contextFacade = contextFacade;
    }

    @GetMapping
    @PreAuthorize("@notificationAccess.canReadInbox(authentication, #recipientId)")
    public List<NotificationDTO> findNotifications(@PathVariable String recipientId) {
        return contextFacade.findNotificationsByRecipientId(recipientId);
    }
}
