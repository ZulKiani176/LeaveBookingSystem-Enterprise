package uk.ac.staffs.leavebooking.leave.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leave_allowance")
public class LeaveAllowanceJpa {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "staff_member_id", nullable = false, length = 100)
    private String staffMemberId;

    @Column(name = "first_name", nullable = false, length = 20)
    private String firstName;

    @Column(name = "surname", nullable = false, length = 20)
    private String surname;

    @Column(name = "manager_id", nullable = false, length = 100)
    private String managerId;

    @Column(name = "business_year_start", nullable = false)
    private LocalDate businessYearStart;

    @Column(name = "business_year_end", nullable = false)
    private LocalDate businessYearEnd;

    @Column(name = "base_entitlement", nullable = false, precision = 5, scale = 1)
    private BigDecimal baseEntitlement;

    @Column(name = "carried_over_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal carriedOverDays;

    @Column(name = "remaining_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal remainingDays;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected LeaveAllowanceJpa() {
    }

    public LeaveAllowanceJpa(
            String id,
            String staffMemberId,
            String firstName,
            String surname,
            String managerId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd,
            int annualEntitlement,
            int remainingDays
    ) {
        this(
                id, staffMemberId, firstName, surname, managerId,
                businessYearStart, businessYearEnd,
                BigDecimal.valueOf(annualEntitlement), BigDecimal.ZERO,
                BigDecimal.valueOf(remainingDays), null
        );
    }

    public LeaveAllowanceJpa(
            String id,
            String staffMemberId,
            String firstName,
            String surname,
            String managerId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd,
            BigDecimal baseEntitlement,
            BigDecimal carriedOverDays,
            BigDecimal remainingDays,
            Long version
    ) {
        this.id = id;
        this.staffMemberId = staffMemberId;
        this.firstName = firstName;
        this.surname = surname;
        this.managerId = managerId;
        this.businessYearStart = businessYearStart;
        this.businessYearEnd = businessYearEnd;
        this.baseEntitlement = baseEntitlement;
        this.carriedOverDays = carriedOverDays;
        this.remainingDays = remainingDays;
        this.version = version;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStaffMemberId() { return staffMemberId; }
    public void setStaffMemberId(String staffMemberId) { this.staffMemberId = staffMemberId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }
    public LocalDate getBusinessYearStart() { return businessYearStart; }
    public void setBusinessYearStart(LocalDate value) { this.businessYearStart = value; }
    public LocalDate getBusinessYearEnd() { return businessYearEnd; }
    public void setBusinessYearEnd(LocalDate value) { this.businessYearEnd = value; }
    public BigDecimal getBaseEntitlement() { return baseEntitlement; }
    public void setBaseEntitlement(BigDecimal value) { this.baseEntitlement = value; }
    public BigDecimal getCarriedOverDays() { return carriedOverDays; }
    public void setCarriedOverDays(BigDecimal value) { this.carriedOverDays = value; }
    public BigDecimal getTotalEntitlement() { return baseEntitlement.add(carriedOverDays); }
    public BigDecimal getRemainingLeaveDays() { return remainingDays; }
    public void setRemainingDays(BigDecimal value) { this.remainingDays = value; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @Deprecated(forRemoval = false)
    public int getAnnualEntitlement() { return getTotalEntitlement().intValueExact(); }

    @Deprecated(forRemoval = false)
    public int getRemainingDays() { return remainingDays.intValueExact(); }

    @Deprecated(forRemoval = false)
    public void setAnnualEntitlement(int annualEntitlement) {
        this.baseEntitlement = BigDecimal.valueOf(annualEntitlement);
        this.carriedOverDays = BigDecimal.ZERO;
    }
}
