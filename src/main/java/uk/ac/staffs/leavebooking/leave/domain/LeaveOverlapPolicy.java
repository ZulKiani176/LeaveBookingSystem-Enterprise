package uk.ac.staffs.leavebooking.leave.domain;

public final class LeaveOverlapPolicy {
    private LeaveOverlapPolicy() {
    }

    public static boolean conflicts(
            LeavePeriod requestedPeriod,
            LeaveDayPortion requestedPortion,
            LeavePeriod existingPeriod,
            LeaveDayPortion existingPortion
    ) {
        boolean datesOverlap = !requestedPeriod.endDate().isBefore(existingPeriod.startDate())
                && !requestedPeriod.startDate().isAfter(existingPeriod.endDate());
        if (!datesOverlap) {
            return false;
        }
        boolean complementaryHalfDays = requestedPeriod.startDate().equals(requestedPeriod.endDate())
                && existingPeriod.startDate().equals(existingPeriod.endDate())
                && requestedPeriod.startDate().equals(existingPeriod.startDate())
                && requestedPortion.isHalfDay()
                && existingPortion.isHalfDay()
                && requestedPortion != existingPortion;
        return !complementaryHalfDays;
    }
}
