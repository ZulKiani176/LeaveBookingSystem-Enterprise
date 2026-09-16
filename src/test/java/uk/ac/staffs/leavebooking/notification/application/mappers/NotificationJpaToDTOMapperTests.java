package uk.ac.staffs.leavebooking.notification.application.mappers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.notification.application.NotificationType;
import uk.ac.staffs.leavebooking.notification.application.dto.NotificationDTO;
import uk.ac.staffs.leavebooking.notification.infrastructure.entities.NotificationJpa;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Notification database To responseMapper")
class NotificationJpaToDTOMapperTests {
    @Test
    @DisplayName("Every persisted notification field maps to the API DTO")
    void everyFieldMaps() {
        Instant createdAt = Instant.parse("2026-08-25T10:00:00Z");
        NotificationJpa source = new NotificationJpa(
                "notification-1",
                7L,
                "staff-1",
                "request-1",
                NotificationType.STAFF_LEAVE_APPROVED,
                "Approved",
                createdAt
        );

        NotificationDTO result = NotificationJpaToDTOMapper.map(source);

        assertEquals("notification-1", result.id());
        assertEquals(7L, result.sourceEventId());
        assertEquals("staff-1", result.recipientId());
        assertEquals("request-1", result.leaveRequestId());
        assertEquals(NotificationType.STAFF_LEAVE_APPROVED, result.type());
        assertEquals("Approved", result.message());
        assertEquals(createdAt, result.createdAt());
    }

    @Test
    @DisplayName("Null persistence input is rejected")
    void nullInputIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                NotificationJpaToDTOMapper.map(null)
        );

        assertEquals(NotificationJpaToDTOMapper.NOTIFICATION_NOT_NULL, exception.getMessage());
    }
}
