package uk.ac.staffs.leavebooking.notification;

import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.notification.application.NotificationService;
import uk.ac.staffs.leavebooking.notification.application.dto.NotificationDTO;

import java.util.List;

@Component("notificationContextFacade")
public class ContextFacade {
    private final NotificationService notificationService;

    public ContextFacade(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public List<NotificationDTO> findNotificationsByRecipientId(String recipientId) {
        return notificationService.findByRecipientId(recipientId);
    }
}
