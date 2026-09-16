package uk.ac.staffs.leavebooking.notification.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.notification.infrastructure.entities.NotificationJpa;
import uk.ac.staffs.leavebooking.notification.infrastructure.repositories.NotificationRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Privacy in sickness notifications")
class NotificationSickLeaveTests {
    @Mock private NotificationRepository repository;

    @Test
    @DisplayName("Sickness creates a manager notification without private reason text")
    void sicknessCreatesPrivacySafeManagerNotification() {
        when(repository.existsBySourceEventId(42L)).thenReturn(false);

        new NotificationService(repository).createManagerSickLeaveRecordedNotification(event());

        ArgumentCaptor<NotificationJpa> captor = ArgumentCaptor.forClass(NotificationJpa.class);
        verify(repository).save(captor.capture());
        NotificationJpa saved = captor.getValue();
        assertEquals("manager-1", saved.getRecipientId());
        assertEquals(NotificationType.MANAGER_SICK_LEAVE_RECORDED, saved.getNotificationType());
        assertEquals("sick-1", saved.getLeaveRequestId());
        assertFalse(saved.getMessage().contains("Migraine"));
    }

    @Test
    @DisplayName("A redelivered sickness event does not duplicate its notification")
    void duplicateSicknessEventIsIdempotent() {
        when(repository.existsBySourceEventId(42L)).thenReturn(true);

        new NotificationService(repository).createManagerSickLeaveRecordedNotification(event());

        verify(repository, never()).save(any());
    }

    private SickLeaveRecordedIntegrationEvent event() {
        LocalDate date = LocalDate.parse("2026-08-24");
        return new SickLeaveRecordedIntegrationEvent(
                42L, date, "sick-1", "staff-1", "manager-1",
                date, date.plusDays(1),
                IntegrationLeaveType.SICK,
                IntegrationLeaveDayPortion.FULL_DAY,
                java.math.BigDecimal.ZERO,
                IntegrationLeaveStatus.RECORDED
        );
    }
}
