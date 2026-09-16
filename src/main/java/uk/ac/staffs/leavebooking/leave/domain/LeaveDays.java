package uk.ac.staffs.leavebooking.leave.domain;

import uk.ac.staffs.leavebooking.common.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record LeaveDays(BigDecimal value) implements ValueObject, Comparable<LeaveDays> {
    public static final String VALUE_NOT_NULL = "Leave days cannot be null";
    public static final String VALUE_NOT_NEGATIVE = "Leave days cannot be negative";
    public static final String VALUE_NOT_HALF_DAY_INCREMENT =
            "Leave days must be in whole-day or half-day increments";
    public static final String VALUE_EXCEEDS_CAPACITY =
            "Leave days cannot exceed 9999.5";
    private static final BigDecimal MAX_VALUE = new BigDecimal("9999.5");

    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");
    private static final LeaveDays ZERO = new LeaveDays(BigDecimal.ZERO);

    public LeaveDays {
        value = argumentNotNull(value, VALUE_NOT_NULL);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(VALUE_NOT_NEGATIVE);
        }
        if (value.compareTo(MAX_VALUE) > 0) {
            throw new IllegalArgumentException(VALUE_EXCEEDS_CAPACITY);
        }
        try {
            value = value.setScale(1, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(VALUE_NOT_HALF_DAY_INCREMENT, exception);
        }
        if (value.remainder(HALF_DAY).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(VALUE_NOT_HALF_DAY_INCREMENT);
        }
    }

    public static LeaveDays zero() {
        return ZERO;
    }

    public static LeaveDays of(int wholeDays) {
        return new LeaveDays(BigDecimal.valueOf(wholeDays));
    }

    public static LeaveDays of(String days) {
        return new LeaveDays(new BigDecimal(days));
    }

    public LeaveDays add(LeaveDays other) {
        return new LeaveDays(value.add(argumentNotNull(other, VALUE_NOT_NULL).value));
    }

    public LeaveDays subtract(LeaveDays other) {
        return new LeaveDays(value.subtract(argumentNotNull(other, VALUE_NOT_NULL).value));
    }

    public LeaveDays min(LeaveDays other) {
        LeaveDays nonNullOther = argumentNotNull(other, VALUE_NOT_NULL);
        return compareTo(nonNullOther) <= 0 ? this : nonNullOther;
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    @Override
    public int compareTo(LeaveDays other) {
        return value.compareTo(argumentNotNull(other, VALUE_NOT_NULL).value);
    }
}
