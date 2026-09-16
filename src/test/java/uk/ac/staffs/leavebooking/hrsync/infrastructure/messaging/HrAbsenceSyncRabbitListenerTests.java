package uk.ac.staffs.leavebooking.hrsync.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.hrsync.application.HrAbsenceSyncService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("HR Absence Sync Rabbit Listener")
class HrAbsenceSyncRabbitListenerTests {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 14);
    @Mock private HrAbsenceSyncService service;

    @Test
    @DisplayName("Approved leave, cancellations and sickness are sent to HR")
    void delegatesEveryBoundHrSyncEventType() {
        HrAbsenceSyncRabbitListener listener = new HrAbsenceSyncRabbitListener(service);
        var approved = new LeaveRequestApprovedIntegrationEvent(
                DATE, "request", "staff", DATE, DATE,
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE);
        var cancelled = new LeaveRequestCancelledIntegrationEvent(
                DATE, "request", "staff", DATE, DATE, "APPROVED",
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE);
        var sick = new SickLeaveRecordedIntegrationEvent(
                DATE, "request", "staff", "manager", DATE, DATE);

        listener.receive(approved);
        listener.receive(cancelled);
        listener.receive(sick);

        verify(service).record(approved);
        verify(service).record(cancelled);
        verify(service).record(sick);
    }
}
