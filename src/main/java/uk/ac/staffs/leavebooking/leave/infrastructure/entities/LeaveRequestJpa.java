package uk.ac.staffs.leavebooking.leave.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "leave_request")
public class LeaveRequestJpa {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "staff_member_id", nullable = false, length = 100)
    private String staffMemberId;

    @Column(name = "manager_id", nullable = false, length = 100)
    private String managerId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_portion", nullable = false, length = 20)
    private LeaveDayPortion dayPortion;

    @Column(name = "charged_leave_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal chargedLeaveDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LeaveStatus status;

    @Column(name = "decision_comment", length = 500)
    private String decisionComment;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected LeaveRequestJpa() {
    }

    public LeaveRequestJpa(
            String id, String staffMemberId, String managerId,
            LocalDate startDate, LocalDate endDate, String reason,
            LeaveType leaveType, LeaveStatus status
    ) {
        this(
                id, staffMemberId, managerId, startDate, endDate, reason,
                leaveType, LeaveDayPortion.FULL_DAY,
                leaveType == LeaveType.SICK
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(Math.max(
                                1L,
                                ChronoUnit.DAYS.between(startDate, endDate) + 1L
                        )),
                status, null, null
        );
    }

    public LeaveRequestJpa(
            String id, String staffMemberId, String managerId,
            LocalDate startDate, LocalDate endDate, String reason,
            LeaveType leaveType, LeaveDayPortion dayPortion,
            BigDecimal chargedLeaveDays, LeaveStatus status,
            String decisionComment, Long version
    ) {
        this.id = id;
        this.staffMemberId = staffMemberId;
        this.managerId = managerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.leaveType = leaveType;
        this.dayPortion = dayPortion;
        this.chargedLeaveDays = chargedLeaveDays;
        this.status = status;
        this.decisionComment = decisionComment;
        this.version = version;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStaffMemberId() { return staffMemberId; }
    public void setStaffMemberId(String value) { this.staffMemberId = value; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String value) { this.managerId = value; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { this.startDate = value; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { this.endDate = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }
    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType value) { this.leaveType = value; }
    public LeaveDayPortion getDayPortion() { return dayPortion; }
    public void setDayPortion(LeaveDayPortion value) { this.dayPortion = value; }
    public BigDecimal getChargedLeaveDays() { return chargedLeaveDays; }
    public void setChargedLeaveDays(BigDecimal value) { this.chargedLeaveDays = value; }
    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus value) { this.status = value; }
    public String getDecisionComment() { return decisionComment; }
    public void setDecisionComment(String value) { this.decisionComment = value; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { this.version = value; }
}
