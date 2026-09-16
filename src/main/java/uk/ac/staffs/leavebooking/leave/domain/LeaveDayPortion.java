package uk.ac.staffs.leavebooking.leave.domain;

public enum LeaveDayPortion {
    FULL_DAY,
    MORNING,
    AFTERNOON;

    public boolean isHalfDay() {
        return this != FULL_DAY;
    }
}
