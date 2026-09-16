package uk.ac.staffs.leavebooking.leave.domain;

import uk.ac.staffs.leavebooking.common.AggregateRoot;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveAllowanceException;

import java.math.BigDecimal;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public class LeaveAllowance extends AggregateRoot<LeaveAllowance> {
    public static final LeaveDays MAX_CARRY_OVER = LeaveDays.of("5.0");
    public static final String STAFF_MEMBER_ID_NOT_EMPTY = "Staff member identity cannot be empty";
    public static final String FULL_NAME_NOT_NULL = "Staff member full name cannot be null";
    public static final String MANAGER_ID_NOT_EMPTY = "Manager identity cannot be empty";
    public static final String BUSINESS_YEAR_NOT_NULL = "Business year cannot be null";
    public static final String BASE_ENTITLEMENT_NOT_NULL = "Base entitlement cannot be null";
    public static final String CARRIED_OVER_DAYS_NOT_NULL = "Carried-over leave cannot be null";
    public static final String DEDUCTION_NOT_POSITIVE = "Days to deduct must be greater than zero";
    public static final String INSUFFICIENT_REMAINING_ALLOWANCE =
            "Cannot deduct more days than the remaining allowance";
    public static final String RESTORE_NOT_POSITIVE = "Days to restore must be greater than zero";
    public static final String RESTORE_EXCEEDS_ENTITLEMENT =
            "Restored days cannot exceed the total entitlement";
    public static final String ENTITLEMENT_BELOW_USED_DAYS =
            "Base entitlement and carry-over cannot be less than leave already used";
    public static final String REMAINING_DAYS_EXCEED_ENTITLEMENT =
            "Remaining days cannot exceed total entitlement";
    public static final String CARRY_OVER_EXCEEDS_MAXIMUM = "Carried-over leave cannot exceed 5 days";
    @Deprecated(forRemoval = false)
    public static final String ANNUAL_ENTITLEMENT_NOT_NEGATIVE = LeaveDays.VALUE_NOT_NEGATIVE;
    @Deprecated(forRemoval = false)
    public static final String REMAINING_DAYS_NOT_NEGATIVE = LeaveDays.VALUE_NOT_NEGATIVE;

    private final String staffMemberId;
    private final FullName fullName;
    private final String managerId;
    private final BusinessYear businessYear;
    private LeaveDays baseEntitlement;
    private LeaveDays carriedOverDays;
    private LeaveDays remainingDays;

    public LeaveAllowance(
            Identity<LeaveAllowance> id,
            String staffMemberId,
            FullName fullName,
            String managerId,
            BusinessYear businessYear,
            int baseEntitlement
    ) {
        this(
                id, staffMemberId, fullName, managerId, businessYear,
                validatedLegacyDays(baseEntitlement, ANNUAL_ENTITLEMENT_NOT_NEGATIVE)
        );
    }

    public LeaveAllowance(
            Identity<LeaveAllowance> id,
            String staffMemberId,
            FullName fullName,
            String managerId,
            BusinessYear businessYear,
            BigDecimal baseEntitlement
    ) {
        this(id, staffMemberId, fullName, managerId, businessYear, new LeaveDays(baseEntitlement));
    }

    public LeaveAllowance(
            Identity<LeaveAllowance> id,
            String staffMemberId,
            FullName fullName,
            String managerId,
            BusinessYear businessYear,
            LeaveDays baseEntitlement
    ) {
        this(
                id,
                staffMemberId,
                fullName,
                managerId,
                businessYear,
                baseEntitlement,
                LeaveDays.zero(),
                baseEntitlement
        );
    }

    private LeaveAllowance(
            Identity<LeaveAllowance> id,
            String staffMemberId,
            FullName fullName,
            String managerId,
            BusinessYear businessYear,
            LeaveDays baseEntitlement,
            LeaveDays carriedOverDays,
            LeaveDays remainingDays
    ) {
        super(id);
        this.staffMemberId = argumentNotEmpty(staffMemberId, STAFF_MEMBER_ID_NOT_EMPTY);
        this.fullName = argumentNotNull(fullName, FULL_NAME_NOT_NULL);
        this.managerId = argumentNotEmpty(managerId, MANAGER_ID_NOT_EMPTY);
        this.businessYear = argumentNotNull(businessYear, BUSINESS_YEAR_NOT_NULL);
        this.baseEntitlement = argumentNotNull(baseEntitlement, BASE_ENTITLEMENT_NOT_NULL);
        this.carriedOverDays = validateCarryOver(carriedOverDays);
        this.remainingDays = argumentNotNull(remainingDays, LeaveDays.VALUE_NOT_NULL);
        validateRemainingDays(this.remainingDays, totalEntitlement());
    }

    public static LeaveAllowance reconstitute(
            Identity<LeaveAllowance> id,
            String staffMemberId,
            FullName fullName,
            String managerId,
            BusinessYear businessYear,
            int annualEntitlement,
            int remainingDays
    ) {
        if (annualEntitlement < 0) {
            throw new InvalidLeaveAllowanceException(ANNUAL_ENTITLEMENT_NOT_NEGATIVE);
        }
        if (remainingDays < 0) {
            throw new InvalidLeaveAllowanceException(REMAINING_DAYS_NOT_NEGATIVE);
        }
        return reconstitute(
                id, staffMemberId, fullName, managerId, businessYear,
                LeaveDays.of(annualEntitlement), LeaveDays.zero(), LeaveDays.of(remainingDays)
        );
    }

    public static LeaveAllowance reconstitute(
            Identity<LeaveAllowance> id,
            String staffMemberId,
            FullName fullName,
            String managerId,
            BusinessYear businessYear,
            LeaveDays baseEntitlement,
            LeaveDays carriedOverDays,
            LeaveDays remainingDays
    ) {
        return new LeaveAllowance(
                id, staffMemberId, fullName, managerId, businessYear,
                baseEntitlement, carriedOverDays, remainingDays
        );
    }

    public String staffMemberId() { return staffMemberId; }
    public FullName fullName() { return fullName; }
    public String managerId() { return managerId; }
    public BusinessYear businessYear() { return businessYear; }
    public LeaveDays baseEntitlement() { return baseEntitlement; }
    public LeaveDays carriedOverDays() { return carriedOverDays; }
    public LeaveDays totalEntitlement() { return baseEntitlement.add(carriedOverDays); }
    public LeaveDays remainingLeaveDays() { return remainingDays; }
    public LeaveDays usedLeaveDays() { return totalEntitlement().subtract(remainingDays); }

    @Deprecated(forRemoval = false)
    public int annualEntitlement() { return baseEntitlement.value().intValueExact(); }
    @Deprecated(forRemoval = false)
    public int remainingDays() { return remainingDays.value().intValueExact(); }
    @Deprecated(forRemoval = false)
    public int usedDays() { return usedLeaveDays().value().intValueExact(); }

    public void deduct(int days) {
        if (days <= 0) {
            throw new InvalidLeaveAllowanceException(DEDUCTION_NOT_POSITIVE);
        }
        deduct(LeaveDays.of(days));
    }

    public void deduct(LeaveDays days) {
        LeaveDays deduction = argumentNotNull(days, LeaveDays.VALUE_NOT_NULL);
        if (!deduction.isPositive()) {
            throw new InvalidLeaveAllowanceException(DEDUCTION_NOT_POSITIVE);
        }
        if (deduction.compareTo(remainingDays) > 0) {
            throw new InvalidLeaveAllowanceException(INSUFFICIENT_REMAINING_ALLOWANCE);
        }
        remainingDays = remainingDays.subtract(deduction);
    }

    public void restore(int days) {
        if (days <= 0) {
            throw new InvalidLeaveAllowanceException(RESTORE_NOT_POSITIVE);
        }
        restore(LeaveDays.of(days));
    }

    public void restore(LeaveDays days) {
        LeaveDays restoration = argumentNotNull(days, LeaveDays.VALUE_NOT_NULL);
        if (!restoration.isPositive()) {
            throw new InvalidLeaveAllowanceException(RESTORE_NOT_POSITIVE);
        }
        if (restoration.compareTo(totalEntitlement().subtract(remainingDays)) > 0) {
            throw new InvalidLeaveAllowanceException(RESTORE_EXCEEDS_ENTITLEMENT);
        }
        remainingDays = remainingDays.add(restoration);
    }

    public void amendEntitlement(int newEntitlement) {
        if (newEntitlement < 0) {
            throw new InvalidLeaveAllowanceException(ANNUAL_ENTITLEMENT_NOT_NEGATIVE);
        }
        amendBaseEntitlement(LeaveDays.of(newEntitlement));
    }

    public void amendBaseEntitlement(LeaveDays newBaseEntitlement) {
        LeaveDays validated = argumentNotNull(newBaseEntitlement, BASE_ENTITLEMENT_NOT_NULL);
        LeaveDays used = usedLeaveDays();
        LeaveDays newTotal = validated.add(carriedOverDays);
        if (newTotal.compareTo(used) < 0) {
            throw new InvalidLeaveAllowanceException(ENTITLEMENT_BELOW_USED_DAYS);
        }
        baseEntitlement = validated;
        remainingDays = newTotal.subtract(used);
    }

    public void setCarryOver(LeaveDays carryOver) {
        LeaveDays validated = validateCarryOver(carryOver);
        LeaveDays used = usedLeaveDays();
        LeaveDays newTotal = baseEntitlement.add(validated);
        if (newTotal.compareTo(used) < 0) {
            throw new InvalidLeaveAllowanceException(ENTITLEMENT_BELOW_USED_DAYS);
        }
        carriedOverDays = validated;
        remainingDays = newTotal.subtract(used);
    }

    private static LeaveDays validateCarryOver(LeaveDays carryOver) {
        LeaveDays validated = argumentNotNull(carryOver, CARRIED_OVER_DAYS_NOT_NULL);
        if (validated.compareTo(MAX_CARRY_OVER) > 0) {
            throw new InvalidLeaveAllowanceException(CARRY_OVER_EXCEEDS_MAXIMUM);
        }
        return validated;
    }

    private static void validateRemainingDays(LeaveDays remaining, LeaveDays entitlement) {
        if (remaining.compareTo(entitlement) > 0) {
            throw new InvalidLeaveAllowanceException(REMAINING_DAYS_EXCEED_ENTITLEMENT);
        }
    }

    private static LeaveDays validatedLegacyDays(int days, String message) {
        if (days < 0) {
            throw new InvalidLeaveAllowanceException(message);
        }
        return LeaveDays.of(days);
    }
}
