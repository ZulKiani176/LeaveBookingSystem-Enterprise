package uk.ac.staffs.leavebooking.audit.application.dto;

import uk.ac.staffs.leavebooking.audit.domain.LeaveAuditAction;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;

import java.time.LocalDate;

public record LeaveAuditDTO(
        Long sourceEventId,
        String leaveRequestId,
        String staffMemberId,
        LeaveAuditAction action,
        IntegrationLeaveStatus previousStatus,
        IntegrationLeaveStatus newStatus,
        String decisionComment,
        LocalDate occurredOn
) {
}
