package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Whole and half days")
class LeaveDaysTests {
    @Test
    @DisplayName("Whole and half-day values are normalised to one decimal place")
    void validValuesAreNormalised() {
        assertEquals(new BigDecimal("2.0"), LeaveDays.of(2).value());
        assertEquals(new BigDecimal("3.5"), LeaveDays.of("3.5").value());
    }

    @Test
    @DisplayName("Values outside half-day increments are rejected")
    void invalidPrecisionIsRejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LeaveDays.of("24.37")
        );

        assertEquals(LeaveDays.VALUE_NOT_HALF_DAY_INCREMENT, exception.getMessage());
    }

    @Test
    @DisplayName("A decimal tenth that is not a half-day increment is rejected")
    void decimalTenthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> LeaveDays.of("0.3"));
    }

    @Test
    @DisplayName("Negative leave days are rejected")
    void negativeDaysAreRejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LeaveDays.of("-0.5")
        );

        assertEquals(LeaveDays.VALUE_NOT_NEGATIVE, exception.getMessage());
    }

    @Test
    @DisplayName("Leave-day arithmetic remains exact")
    void arithmeticIsExact() {
        LeaveDays result = LeaveDays.of("3.5")
                .add(LeaveDays.of("0.5"))
                .subtract(LeaveDays.of("1.0"));

        assertEquals(LeaveDays.of("3.0"), result);
    }

    @Test
    @DisplayName("Minimum selects the smaller exact value")
    void minimumSelectsSmallerValue() {
        assertEquals(LeaveDays.of("3.5"), LeaveDays.of("8.0").min(LeaveDays.of("3.5")));
        assertTrue(LeaveDays.zero().isZero());
    }
}
