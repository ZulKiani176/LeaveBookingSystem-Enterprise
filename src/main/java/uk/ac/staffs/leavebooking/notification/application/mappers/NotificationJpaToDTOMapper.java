package uk.ac.staffs.leavebooking.notification.application.mappers;

import uk.ac.staffs.leavebooking.notification.application.dto.NotificationDTO;
import uk.ac.staffs.leavebooking.notification.infrastructure.entities.NotificationJpa;

import java.util.Objects;

public final class NotificationJpaToDTOMapper {
    public static final String NOTIFICATION_NOT_NULL = "Notification persistence record cannot be null";

    private NotificationJpaToDTOMapper() {
    }

    public static NotificationDTO map(NotificationJpa notification) {
        NotificationJpa source = Objects.requireNonNull(notification, NOTIFICATION_NOT_NULL);
        return new NotificationDTO(
                source.getId(),
                source.getSourceEventId(),
                source.getRecipientId(),
                source.getLeaveRequestId(),
                source.getNotificationType(),
                source.getMessage(),
                source.getCreatedAt()
        );
    }
}
