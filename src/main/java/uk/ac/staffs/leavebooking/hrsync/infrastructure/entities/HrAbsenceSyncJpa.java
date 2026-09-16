package uk.ac.staffs.leavebooking.hrsync.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uk.ac.staffs.leavebooking.hrsync.domain.HrAbsenceSyncAction;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hr_absence_sync")
public class HrAbsenceSyncJpa {
    @Id
    @Column(name = "source_event_id", nullable = false)
    private Long sourceEventId;

    @Column(name = "leave_request_id", nullable = false, length = 36)
    private String leaveRequestId;

    @Column(name = "staff_member_id", nullable = false, length = 100)
    private String staffMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_action", nullable = false, length = 40)
    private HrAbsenceSyncAction syncAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private IntegrationLeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_portion", nullable = false, length = 20)
    private IntegrationLeaveDayPortion dayPortion;

    @Column(name = "charged_leave_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal chargedLeaveDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IntegrationLeaveStatus status;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    protected HrAbsenceSyncJpa() {
    }

    public HrAbsenceSyncJpa(
            Long sourceEventId,
            String leaveRequestId,
            String staffMemberId,
            HrAbsenceSyncAction syncAction,
            IntegrationLeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            IntegrationLeaveDayPortion dayPortion,
            BigDecimal chargedLeaveDays,
            IntegrationLeaveStatus status,
            LocalDate occurredOn
    ) {
        this.sourceEventId = sourceEventId;
        this.leaveRequestId = leaveRequestId;
        this.staffMemberId = staffMemberId;
        this.syncAction = syncAction;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dayPortion = dayPortion;
        this.chargedLeaveDays = chargedLeaveDays;
        this.status = status;
        this.occurredOn = occurredOn;
    }

    public Long getSourceEventId() { return sourceEventId; }
    public String getLeaveRequestId() { return leaveRequestId; }
    public String getStaffMemberId() { return staffMemberId; }
    public HrAbsenceSyncAction getSyncAction() { return syncAction; }
    public IntegrationLeaveType getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public IntegrationLeaveDayPortion getDayPortion() { return dayPortion; }
    public BigDecimal getChargedLeaveDays() { return chargedLeaveDays; }
    public IntegrationLeaveStatus getStatus() { return status; }
    public LocalDate getOccurredOn() { return occurredOn; }
}
