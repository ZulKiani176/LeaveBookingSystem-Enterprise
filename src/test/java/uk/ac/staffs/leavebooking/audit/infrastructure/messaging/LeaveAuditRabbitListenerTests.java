package uk.ac.staffs.leavebooking.audit.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.audit.application.LeaveAuditService;
import uk.ac.staffs.leavebooking.common.events.integration.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Audit Rabbit Listener")
class LeaveAuditRabbitListenerTests {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 14);
    @Mock private LeaveAuditService service;

    @Test
    @DisplayName("Each supported leave event is sent to the audit service")
    void delegatesEveryBoundLeaveEventType() {
        LeaveAuditRabbitListener listener = new LeaveAuditRabbitListener(service);
        var submitted = new LeaveRequestSubmittedIntegrationEvent(
                DATE, "request", "staff", "manager", DATE, DATE,
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE);
        var approved = new LeaveRequestApprovedIntegrationEvent(
                DATE, "request", "staff", DATE, DATE,
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE);
        var rejected = new LeaveRequestRejectedIntegrationEvent(
                DATE, "request", "staff", DATE, DATE,
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE);
        var referred = new LeaveRequestReferredForHrApprovalIntegrationEvent(
                DATE, "request", "staff");
        var cancelled = new LeaveRequestCancelledIntegrationEvent(
                DATE, "request", "staff", DATE, DATE, "APPROVED",
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE);
        var sick = new SickLeaveRecordedIntegrationEvent(
                DATE, "request", "staff", "manager", DATE, DATE);

        listener.receive(submitted);
        listener.receive(approved);
        listener.receive(rejected);
        listener.receive(referred);
        listener.receive(cancelled);
        listener.receive(sick);

        verify(service).record(submitted);
        verify(service).record(approved);
        verify(service).record(rejected);
        verify(service).record(referred);
        verify(service).record(cancelled);
        verify(service).record(sick);
    }
}
