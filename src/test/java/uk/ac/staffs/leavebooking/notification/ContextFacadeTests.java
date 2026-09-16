package uk.ac.staffs.leavebooking.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.notification.application.NotificationService;
import uk.ac.staffs.leavebooking.notification.application.dto.NotificationDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification public operations")
class ContextFacadeTests {
    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("A notification inbox returns the recipient's messages")
    void facadeDelegatesInboxQuery() {
        List<NotificationDTO> expected = List.of();
        when(notificationService.findByRecipientId("staff-1")).thenReturn(expected);
        ContextFacade facade = new ContextFacade(notificationService);

        List<NotificationDTO> result = facade.findNotificationsByRecipientId("staff-1");

        assertSame(expected, result);
    }
}
