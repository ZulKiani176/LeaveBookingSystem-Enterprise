package uk.ac.staffs.leavebooking.audit.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.audit.domain.LeaveAuditAction;
import uk.ac.staffs.leavebooking.audit.infrastructure.entities.LeaveAuditJpa;
import uk.ac.staffs.leavebooking.audit.infrastructure.repositories.LeaveAuditRepository;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestReferredForHrApprovalIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Audit Service")
class LeaveAuditServiceTests {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 14);
    @Mock private LeaveAuditRepository repository;

    @Test
    @DisplayName("Submitted leave creates a business-readable audit entry")
    void submittedLeaveCreatesAuditEntry() {
        LeaveAuditService service = new LeaveAuditService(repository);

        service.record(submitted(10L));

        LeaveAuditJpa saved = captureSaved();
        assertEquals(10L, saved.getSourceEventId());
        assertEquals(LeaveAuditAction.SUBMITTED, saved.getAction());
        assertEquals(IntegrationLeaveStatus.PENDING, saved.getNewStatus());
    }

    @Test
    @DisplayName("An HR approval records its decision source without free-text payload")
    void hrApprovalRecordsDecisionInformation() {
        LeaveAuditService service = new LeaveAuditService(repository);

        service.record(new LeaveRequestApprovedIntegrationEvent(
                11L, DATE, "request-1", "staff-1", DATE, DATE,
                IntegrationLeaveType.ANNUAL, IntegrationLeaveDayPortion.FULL_DAY,
                BigDecimal.ONE, IntegrationLeaveStatus.APPROVED,
                IntegrationLeaveStatus.PENDING_HR_APPROVAL
        ));

        LeaveAuditJpa saved = captureSaved();
        assertEquals(LeaveAuditAction.HR_APPROVED, saved.getAction());
        assertEquals(IntegrationLeaveStatus.PENDING_HR_APPROVAL, saved.getPreviousStatus());
        assertNull(saved.getDecisionComment());
    }

    @Test
    @DisplayName("HR referral is retained as a separate audit action")
    void hrReferralCreatesAuditEntry() {
        LeaveAuditService service = new LeaveAuditService(repository);

        service.record(new LeaveRequestReferredForHrApprovalIntegrationEvent(
                DATE, "request-1", "staff-1"
        ).withId(12L));

        LeaveAuditJpa saved = captureSaved();
        assertEquals(LeaveAuditAction.REFERRED_FOR_HR_APPROVAL, saved.getAction());
        assertEquals(IntegrationLeaveStatus.PENDING, saved.getPreviousStatus());
        assertEquals(IntegrationLeaveStatus.PENDING_HR_APPROVAL, saved.getNewStatus());
    }

    @Test
    @DisplayName("Duplicate source events are idempotently ignored")
    void duplicateSourceEventIsIgnored() {
        when(repository.existsById(10L)).thenReturn(true);
        LeaveAuditService service = new LeaveAuditService(repository);

        service.record(submitted(10L));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Sickness audit records operational state without a reason field")
    void sicknessAuditIsPrivacySafe() {
        LeaveAuditService service = new LeaveAuditService(repository);

        service.record(new SickLeaveRecordedIntegrationEvent(
                DATE, "sick-1", "staff-1", "manager-1", DATE, DATE
        ).withId(13L));

        LeaveAuditJpa saved = captureSaved();
        assertEquals(LeaveAuditAction.SICK_LEAVE_RECORDED, saved.getAction());
        assertFalse(List.of(LeaveAuditJpa.class.getDeclaredFields()).stream()
                .anyMatch(field -> field.getName().equals("reason")));
    }

    @Test
    @DisplayName("Audit date filters require a complete valid range")
    void auditDateRangeIsValidated() {
        LeaveAuditService service = new LeaveAuditService(repository);

        assertThrows(IllegalArgumentException.class, () ->
                service.find(null, null, DATE, null));
        assertThrows(IllegalArgumentException.class, () ->
                service.find(null, null, DATE, DATE.minusDays(1)));
    }

    private LeaveAuditJpa captureSaved() {
        ArgumentCaptor<LeaveAuditJpa> captor = ArgumentCaptor.forClass(LeaveAuditJpa.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private LeaveRequestSubmittedIntegrationEvent submitted(Long id) {
        return new LeaveRequestSubmittedIntegrationEvent(
                DATE, "request-1", "staff-1", "manager-1", DATE, DATE,
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE
        ).withId(id);
    }
}
