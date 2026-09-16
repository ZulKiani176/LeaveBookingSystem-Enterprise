package uk.ac.staffs.leavebooking.notification.application.dto;

import uk.ac.staffs.leavebooking.notification.application.NotificationType;

import java.time.Instant;

public record NotificationDTO(
        String id,
        Long sourceEventId,
        String recipientId,
        String leaveRequestId,
        NotificationType type,
        String message,
        Instant createdAt
) {
}
