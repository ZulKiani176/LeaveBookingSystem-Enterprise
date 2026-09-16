package uk.ac.staffs.leavebooking.notification.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.notification.application.dto.NotificationDTO;
import uk.ac.staffs.leavebooking.notification.infrastructure.entities.NotificationJpa;
import uk.ac.staffs.leavebooking.notification.infrastructure.repositories.NotificationRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service")
class NotificationServiceTests {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 25);

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Submission creates a manager pending notification")
    void submissionCreatesManagerNotification() {
        notificationService.createManagerPendingNotification(
                new LeaveRequestSubmittedIntegrationEvent(
                        1L, DATE, "request-1", "staff-1", "manager-1", DATE, DATE
                )
        );

        NotificationJpa saved = captureSavedNotification();
        assertEquals(1L, saved.getSourceEventId());
        assertEquals("manager-1", saved.getRecipientId());
        assertEquals("request-1", saved.getLeaveRequestId());
        assertEquals(NotificationType.MANAGER_PENDING_LEAVE, saved.getNotificationType());
        assertEquals("Leave request request-1 is awaiting your approval.", saved.getMessage());
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Approval creates a staff approved notification")
    void approvalCreatesStaffNotification() {
        notificationService.createStaffApprovedNotification(
                new LeaveRequestApprovedIntegrationEvent(
                        2L, DATE, "request-1", "staff-1", DATE, DATE
                )
        );

        NotificationJpa saved = captureSavedNotification();
        assertEquals("staff-1", saved.getRecipientId());
        assertEquals(NotificationType.STAFF_LEAVE_APPROVED, saved.getNotificationType());
        assertEquals("Your leave request request-1 has been approved.", saved.getMessage());
    }

    @Test
    @DisplayName("Rejection creates the justified staff rejected notification")
    void rejectionCreatesStaffNotification() {
        notificationService.createStaffRejectedNotification(
                new LeaveRequestRejectedIntegrationEvent(3L, DATE, "request-1", "staff-1")
        );

        NotificationJpa saved = captureSavedNotification();
        assertEquals("staff-1", saved.getRecipientId());
        assertEquals(NotificationType.STAFF_LEAVE_REJECTED, saved.getNotificationType());
        assertEquals("Your leave request request-1 has been rejected.", saved.getMessage());
    }

    @Test
    @DisplayName("Cancellation creates a staff cancelled notification")
    void cancellationCreatesStaffNotification() {
        notificationService.createStaffCancelledNotification(
                new LeaveRequestCancelledIntegrationEvent(
                        4L, DATE, "request-1", "staff-1", DATE, DATE, "APPROVED"
                )
        );

        NotificationJpa saved = captureSavedNotification();
        assertEquals("staff-1", saved.getRecipientId());
        assertEquals(NotificationType.STAFF_LEAVE_CANCELLED, saved.getNotificationType());
        assertEquals("Your leave request request-1 has been cancelled.", saved.getMessage());
    }

    @Test
    @DisplayName("A redelivered source event does not create a duplicate notification")
    void duplicateSourceEventIsIdempotent() {
        when(notificationRepository.existsBySourceEventId(3L)).thenReturn(true);

        notificationService.createStaffRejectedNotification(
                new LeaveRequestRejectedIntegrationEvent(3L, DATE, "request-1", "staff-1")
        );

        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Recipient inbox maps repository results in repository order")
    void recipientInboxMapsRepositoryResults() {
        NotificationJpa newest = notification("notification-2", 2L, Instant.parse("2026-08-25T11:00:00Z"));
        NotificationJpa oldest = notification("notification-1", 1L, Instant.parse("2026-08-25T10:00:00Z"));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc("staff-1"))
                .thenReturn(List.of(newest, oldest));

        List<NotificationDTO> results = notificationService.findByRecipientId(" staff-1 ");

        assertEquals(List.of("notification-2", "notification-1"),
                results.stream().map(NotificationDTO::id).toList());
    }

    private NotificationJpa captureSavedNotification() {
        ArgumentCaptor<NotificationJpa> captor = ArgumentCaptor.forClass(NotificationJpa.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }

    private NotificationJpa notification(String id, Long sourceEventId, Instant createdAt) {
        return new NotificationJpa(
                id,
                sourceEventId,
                "staff-1",
                "request-1",
                NotificationType.STAFF_LEAVE_APPROVED,
                "Approved",
                createdAt
        );
    }
}
