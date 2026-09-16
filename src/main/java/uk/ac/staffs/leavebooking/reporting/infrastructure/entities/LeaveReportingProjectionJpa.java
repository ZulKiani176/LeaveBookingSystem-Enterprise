package uk.ac.staffs.leavebooking.reporting.infrastructure.entities;

import jakarta.persistence.*;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leave_reporting_projection")
public class LeaveReportingProjectionJpa {
    @Id
    @Column(name = "leave_request_id", nullable = false, length = 36)
    private String leaveRequestId;

    @Column(name = "source_event_id", nullable = false, unique = true)
    private Long sourceEventId;

    @Column(name = "staff_member_id", nullable = false, length = 100)
    private String staffMemberId;

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

    protected LeaveReportingProjectionJpa() {
    }

    public LeaveReportingProjectionJpa(
            String leaveRequestId, Long sourceEventId, String staffMemberId,
            IntegrationLeaveType leaveType, LocalDate startDate, LocalDate endDate,
            IntegrationLeaveDayPortion dayPortion, BigDecimal chargedLeaveDays,
            IntegrationLeaveStatus status, LocalDate occurredOn
    ) {
        this.leaveRequestId = leaveRequestId;
        this.sourceEventId = sourceEventId;
        this.staffMemberId = staffMemberId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dayPortion = dayPortion;
        this.chargedLeaveDays = chargedLeaveDays;
        this.status = status;
        this.occurredOn = occurredOn;
    }

    public String getLeaveRequestId() { return leaveRequestId; }
    public Long getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(Long value) { sourceEventId = value; }
    public String getStaffMemberId() { return staffMemberId; }
    public void setStaffMemberId(String value) { staffMemberId = value; }
    public IntegrationLeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(IntegrationLeaveType value) { leaveType = value; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { startDate = value; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate = value; }
    public IntegrationLeaveDayPortion getDayPortion() { return dayPortion; }
    public void setDayPortion(IntegrationLeaveDayPortion value) { dayPortion = value; }
    public BigDecimal getChargedLeaveDays() { return chargedLeaveDays; }
    public void setChargedLeaveDays(BigDecimal value) { chargedLeaveDays = value; }
    public IntegrationLeaveStatus getStatus() { return status; }
    public void setStatus(IntegrationLeaveStatus value) { status = value; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate value) { occurredOn = value; }
}
