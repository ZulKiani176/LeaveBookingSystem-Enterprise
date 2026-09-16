package uk.ac.staffs.leavebooking.audit.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uk.ac.staffs.leavebooking.audit.domain.LeaveAuditAction;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;

import java.time.LocalDate;

@Entity
@Table(name = "leave_audit")
public class LeaveAuditJpa {
    @Id
    @Column(name = "source_event_id", nullable = false)
    private Long sourceEventId;

    @Column(name = "leave_request_id", nullable = false, length = 36)
    private String leaveRequestId;

    @Column(name = "staff_member_id", nullable = false, length = 100)
    private String staffMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private LeaveAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private IntegrationLeaveStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private IntegrationLeaveStatus newStatus;

    @Column(name = "decision_comment", length = 500)
    private String decisionComment;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    protected LeaveAuditJpa() {
    }

    public LeaveAuditJpa(
            Long sourceEventId,
            String leaveRequestId,
            String staffMemberId,
            LeaveAuditAction action,
            IntegrationLeaveStatus previousStatus,
            IntegrationLeaveStatus newStatus,
            String decisionComment,
            LocalDate occurredOn
    ) {
        this.sourceEventId = sourceEventId;
        this.leaveRequestId = leaveRequestId;
        this.staffMemberId = staffMemberId;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.decisionComment = decisionComment;
        this.occurredOn = occurredOn;
    }

    public Long getSourceEventId() { return sourceEventId; }
    public String getLeaveRequestId() { return leaveRequestId; }
    public String getStaffMemberId() { return staffMemberId; }
    public LeaveAuditAction getAction() { return action; }
    public IntegrationLeaveStatus getPreviousStatus() { return previousStatus; }
    public IntegrationLeaveStatus getNewStatus() { return newStatus; }
    public String getDecisionComment() { return decisionComment; }
    public LocalDate getOccurredOn() { return occurredOn; }
}
