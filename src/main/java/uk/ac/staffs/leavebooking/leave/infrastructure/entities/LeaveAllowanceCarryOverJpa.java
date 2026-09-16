package uk.ac.staffs.leavebooking.leave.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "leave_allowance_carry_over",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_leave_allowance_carry_source_target",
                columnNames = {"source_allowance_id", "target_allowance_id"}
        )
)
public class LeaveAllowanceCarryOverJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_member_id", nullable = false, length = 100)
    private String staffMemberId;

    @Column(name = "source_allowance_id", nullable = false, length = 36)
    private String sourceAllowanceId;

    @Column(name = "target_allowance_id", nullable = false, length = 36)
    private String targetAllowanceId;

    @Column(name = "carried_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal carriedDays;

    @Column(name = "applied_on", nullable = false)
    private LocalDate appliedOn;

    protected LeaveAllowanceCarryOverJpa() {
    }

    public LeaveAllowanceCarryOverJpa(
            String staffMemberId,
            String sourceAllowanceId,
            String targetAllowanceId,
            BigDecimal carriedDays,
            LocalDate appliedOn
    ) {
        this.staffMemberId = staffMemberId;
        this.sourceAllowanceId = sourceAllowanceId;
        this.targetAllowanceId = targetAllowanceId;
        this.carriedDays = carriedDays;
        this.appliedOn = appliedOn;
    }

    public Long getId() { return id; }
    public String getStaffMemberId() { return staffMemberId; }
    public String getSourceAllowanceId() { return sourceAllowanceId; }
    public String getTargetAllowanceId() { return targetAllowanceId; }
    public BigDecimal getCarriedDays() { return carriedDays; }
    public LocalDate getAppliedOn() { return appliedOn; }
}
