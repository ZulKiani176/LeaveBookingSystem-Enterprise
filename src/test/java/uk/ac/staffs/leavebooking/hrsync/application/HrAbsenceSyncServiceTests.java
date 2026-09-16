package uk.ac.staffs.leavebooking.hrsync.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.hrsync.domain.HrAbsenceSyncAction;
import uk.ac.staffs.leavebooking.hrsync.infrastructure.entities.HrAbsenceSyncJpa;
import uk.ac.staffs.leavebooking.hrsync.infrastructure.repositories.HrAbsenceSyncRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HR Absence Sync Service")
class HrAbsenceSyncServiceTests {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 14);
    @Mock private HrAbsenceSyncRepository repository;

    @Test
    @DisplayName("Approved annual leave creates an outbound HR synchronisation record")
    void approvedAnnualLeaveCreatesSyncRecord() {
        HrAbsenceSyncService service = new HrAbsenceSyncService(repository);

        service.record(approved(20L));

        HrAbsenceSyncJpa saved = captureSaved();
        assertEquals(HrAbsenceSyncAction.ANNUAL_LEAVE_APPROVED, saved.getSyncAction());
        assertEquals(IntegrationLeaveStatus.APPROVED, saved.getStatus());
        assertEquals(0, saved.getChargedLeaveDays().compareTo(new BigDecimal("1.0")));
    }

    @Test
    @DisplayName("Cancellation is exported only when the previous status was APPROVED")
    void onlyApprovedCancellationCreatesSyncRecord() {
        HrAbsenceSyncService service = new HrAbsenceSyncService(repository);
        LeaveRequestCancelledIntegrationEvent approvedCancellation = cancelled(21L, "APPROVED");

        service.record(approvedCancellation);

        assertEquals(HrAbsenceSyncAction.APPROVED_LEAVE_CANCELLED,
                captureSaved().getSyncAction());
    }

    @Test
    @DisplayName("Pending cancellation is acknowledged without creating an HR absence record")
    void pendingCancellationIsIgnored() {
        HrAbsenceSyncService service = new HrAbsenceSyncService(repository);

        service.record(cancelled(22L, "PENDING"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Sickness is exported with zero charge and no reason field")
    void sicknessSyncIsPrivacySafe() {
        HrAbsenceSyncService service = new HrAbsenceSyncService(repository);

        service.record(new SickLeaveRecordedIntegrationEvent(
                DATE, "sick-1", "staff-1", "manager-1", DATE, DATE.plusDays(1)
        ).withId(23L));

        HrAbsenceSyncJpa saved = captureSaved();
        assertEquals(HrAbsenceSyncAction.SICK_LEAVE_RECORDED, saved.getSyncAction());
        assertEquals(IntegrationLeaveType.SICK, saved.getLeaveType());
        assertEquals(0, saved.getChargedLeaveDays().compareTo(BigDecimal.ZERO));
        assertFalse(List.of(HrAbsenceSyncJpa.class.getDeclaredFields()).stream()
                .anyMatch(field -> field.getName().equals("reason")));
    }

    @Test
    @DisplayName("Duplicate source events are idempotently ignored")
    void duplicateSourceEventIsIgnored() {
        when(repository.existsById(20L)).thenReturn(true);
        HrAbsenceSyncService service = new HrAbsenceSyncService(repository);

        service.record(approved(20L));

        verify(repository, never()).save(any());
    }

    private HrAbsenceSyncJpa captureSaved() {
        ArgumentCaptor<HrAbsenceSyncJpa> captor =
                ArgumentCaptor.forClass(HrAbsenceSyncJpa.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private LeaveRequestApprovedIntegrationEvent approved(Long id) {
        return new LeaveRequestApprovedIntegrationEvent(
                DATE, "request-1", "staff-1", DATE, DATE,
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE
        ).withId(id);
    }

    private LeaveRequestCancelledIntegrationEvent cancelled(Long id, String previousStatus) {
        return new LeaveRequestCancelledIntegrationEvent(
                DATE, "request-1", "staff-1", DATE, DATE, previousStatus,
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE
        ).withId(id);
    }
}
