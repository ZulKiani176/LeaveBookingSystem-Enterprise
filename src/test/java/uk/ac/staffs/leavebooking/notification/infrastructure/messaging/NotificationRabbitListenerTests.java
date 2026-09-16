package uk.ac.staffs.leavebooking.notification.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.notification.application.NotificationService;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Rabbit Listener")
class NotificationRabbitListenerTests {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 25);

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("Submitted events are forwarded to manager notification handling")
    void submittedEventIsForwarded() {
        LeaveRequestSubmittedIntegrationEvent event = new LeaveRequestSubmittedIntegrationEvent(
                1L, DATE, "request-1", "staff-1", "manager-1", DATE, DATE
        );

        listener().receive(event);

        verify(notificationService).createManagerPendingNotification(event);
    }

    @Test
    @DisplayName("Approved events are forwarded to staff notification handling")
    void approvedEventIsForwarded() {
        LeaveRequestApprovedIntegrationEvent event = new LeaveRequestApprovedIntegrationEvent(
                2L, DATE, "request-1", "staff-1", DATE, DATE
        );

        listener().receive(event);

        verify(notificationService).createStaffApprovedNotification(event);
    }

    @Test
    @DisplayName("Rejected events are forwarded to staff notification handling")
    void rejectedEventIsForwarded() {
        LeaveRequestRejectedIntegrationEvent event = new LeaveRequestRejectedIntegrationEvent(
                3L, DATE, "request-1", "staff-1"
        );

        listener().receive(event);

        verify(notificationService).createStaffRejectedNotification(event);
    }

    @Test
    @DisplayName("Cancelled events are forwarded to staff notification handling")
    void cancelledEventIsForwarded() {
        LeaveRequestCancelledIntegrationEvent event = new LeaveRequestCancelledIntegrationEvent(
                4L, DATE, "request-1", "staff-1", DATE, DATE, "APPROVED"
        );

        listener().receive(event);

        verify(notificationService).createStaffCancelledNotification(event);
    }

    private NotificationRabbitListener listener() {
        return new NotificationRabbitListener(notificationService);
    }
}
